package com.tchalanet.server.core.offlinesync.internal.infra.scheduler;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineEventOutboxJpaEntity;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineEventOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Transactional unit for the offline-sync outbox drain operation.
 *
 * <p>Extracted into its own bean so that {@code @Transactional} is applied via the Spring
 * AOP proxy (self-invocation from the scheduler would bypass it otherwise).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineEventOutboxDrainService {

    private static final int BATCH_SIZE = 50;
    private static final Duration BACKOFF_BASE = Duration.ofSeconds(30);
    private static final Duration BACKOFF_MAX = Duration.ofMinutes(15);

    private final OfflineEventOutboxJpaRepository repo;
    private final JsonUtils jsonUtils;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void drainBatch() {
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
            return (DomainEvent) jsonUtils.readValue(row.getPayloadJson(), clazz);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            throw new IllegalStateException(
                "Failed to deserialise outbox row " + row.getId(), ex);
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
}

