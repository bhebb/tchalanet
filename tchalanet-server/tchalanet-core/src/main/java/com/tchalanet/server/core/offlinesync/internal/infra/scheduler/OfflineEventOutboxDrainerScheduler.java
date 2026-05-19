package com.tchalanet.server.core.offlinesync.internal.infra.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineEventOutboxJpaEntity;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineEventOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Drains the {@code offline_event_outbox} table: deserialises pending events, publishes
 * them via the in-process event bus, and marks them as published in the same transaction.
 *
 * <p>Crash-safe: if the JVM dies between publish and mark, the row stays {@code pending}
 * and the next tick retries. Idempotence on the consumer side
 * ({@link com.tchalanet.server.platform.idempotence.api.ProcessedEventPort}) absorbs the
 * duplicate publish.
 *
 * <p>Cross-pod safety via {@code @SchedulerLock}: a single instance drains at a time.
 * Within the drainer, a pessimistic-write SELECT bounded by {@code findPendingForPublish}
 * + Postgres row locks gives us per-row isolation.
 */
@Component
@ConditionalOnProperty(prefix = "tch.offlinesync", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OfflineEventOutboxDrainerScheduler {

    private static final String ACTOR = "offlinesync:outbox-drainer";
    private static final int BATCH_SIZE = 50;
    private static final Duration BACKOFF_BASE = Duration.ofSeconds(30);
    private static final Duration BACKOFF_MAX = Duration.ofMinutes(15);

    private final OfflineEventOutboxJpaRepository repo;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher eventPublisher;
    private final JobContextBinder binder;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${tch.offlinesync.jobs.outbox-drainer-delay:PT5S}")
    @SchedulerLock(
        name = "offlinesync_outbox_drainer",
        lockAtMostFor = "PT2M",
        lockAtLeastFor = "PT5S")
    public void tick() {
        MDC.put("job", ACTOR);
        try {
            binder.bindPlatform(ACTOR);
            drainBatch();
        } catch (Exception ex) {
            log.warn("offlinesync: outbox drainer top-level failure — {}", ex.getMessage(), ex);
        } finally {
            binder.clear();
            MDC.remove("job");
        }
    }

    @Transactional
    protected void drainBatch() {
        var now = clock.instant();
        List<OfflineEventOutboxJpaEntity> pending =
            repo.findPendingForPublish(now, PageRequest.of(0, BATCH_SIZE));
        if (pending.isEmpty()) return;
        log.debug("offlinesync: outbox drainer picked up {} pending events", pending.size());
        for (var row : pending) {
            try {
                DomainEvent event = deserialize(row);
                eventPublisher.publish(event);
                row.setPublishedAt(clock.instant());
                row.setNextAttemptAt(null);
                row.setLastError(null);
                repo.save(row);
            } catch (Exception ex) {
                row.setAttempts((row.getAttempts() == null ? 0 : row.getAttempts()) + 1);
                row.setLastError(truncate(ex.getMessage()));
                row.setNextAttemptAt(now.plus(backoffFor(row.getAttempts())));
                repo.save(row);
                log.warn("offlinesync: outbox publish failed for {} (attempt={}): {}",
                    row.getEventId(), row.getAttempts(), ex.getMessage());
            }
        }
    }

    private DomainEvent deserialize(OfflineEventOutboxJpaEntity row) {
        try {
            Class<?> clazz = Class.forName(row.getEventClass());
            if (!DomainEvent.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("not a DomainEvent: " + row.getEventClass());
            }
            return (DomainEvent) objectMapper.readValue(row.getPayloadJson(), clazz);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            throw new IllegalStateException(
                "Failed to deserialise outbox row " + row.getId(), ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(
                "JSON parse error on outbox row " + row.getId(), ex);
        }
    }

    private static Duration backoffFor(int attempts) {
        long secs = (long) (BACKOFF_BASE.getSeconds() * Math.pow(2, Math.min(attempts, 10)));
        return Duration.ofSeconds(Math.min(secs, BACKOFF_MAX.getSeconds()));
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }

    /** Type marker kept here for future per-tenant binding if RLS strictness changes. */
    @SuppressWarnings("unused")
    private TenantId asTenantId(OfflineEventOutboxJpaEntity row) {
        return TenantId.of(row.getTenantId());
    }
}
