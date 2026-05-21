package com.tchalanet.server.core.offlinesync.internal.infra.scheduler;

import com.tchalanet.server.common.job.context.JobContextBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler entry-point that drains the {@code offline_event_outbox} table.
 *
 * <p>The actual transactional work is delegated to {@link OfflineEventOutboxDrainService}
 * to avoid Spring AOP self-invocation bypassing {@code @Transactional}.
 *
 * <p>Cross-pod safety via {@code @SchedulerLock}: a single instance drains at a time.
 */
@Component
@ConditionalOnProperty(prefix = "tch.offlinesync", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OfflineEventOutboxDrainerScheduler {

    private static final String ACTOR = "offlinesync:outbox-drainer";

    private final OfflineEventOutboxDrainService drainService;
    private final JobContextBinder binder;

    @Scheduled(fixedDelayString = "${tch.offlinesync.jobs.outbox-drainer-delay:PT5S}")
    @SchedulerLock(
        name = "offlinesync_outbox_drainer",
        lockAtMostFor = "PT2M",
        lockAtLeastFor = "PT5S")
    public void tick() {
        MDC.put("job", ACTOR);
        try {
            binder.bindPlatform(ACTOR);
            drainService.drainBatch();
        } catch (Exception ex) {
            log.warn("offlinesync: outbox drainer top-level failure — {}", ex.getMessage(), ex);
        } finally {
            binder.clear();
            MDC.remove("job");
        }
    }
}
