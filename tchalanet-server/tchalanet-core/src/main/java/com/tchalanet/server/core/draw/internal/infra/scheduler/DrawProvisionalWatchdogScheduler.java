package com.tchalanet.server.core.draw.internal.infra.scheduler;

import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.job.exception.JobSkippedException;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Watchdog that monitors draws stuck in RESULTED state with a PROVISIONAL result.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawProvisionalWatchdogScheduler {

    private static final JobKey DRAW_WATCHDOG_PROVISIONAL = JobKey.of("draw:watchdog:provisional");

    private final DrawReaderPort drawReader;
    private final MeterRegistry meterRegistry;
    private final BatchGate batchGate;
    private final DrawProperties drawProps;
    private final JobContextBinder binder;

    @TchJob("draw:watchdog:provisional")
    @Scheduled(cron = "${tch.draw.watchdog.provisional_cron:0 */15 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_provisional_watchdog", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void checkProvisionalStuck() {
        validateCanRun();

        log.debug("draw.watchdog.provisional tick fired");

        var threshold =
            Duration.ofMinutes(drawProps.getWatchdog().getProvisionalStuckMinutes());
        binder.bindPlatform("draw-watchdog-scheduler");
        try {
            var stuckDraws = drawReader.findResultedWithProvisionalOlderThan(threshold);

            if (stuckDraws.isEmpty()) {
                log.debug("draw.watchdog.provisional no stuck draws threshold={}", threshold);
                return;
            }

            for (var draw : stuckDraws) {
                var slot = draw.drawChannelCode() != null ? draw.drawChannelCode() : "unknown";

                log.warn(
                    "draw.watchdog.provisional stuck drawId={} slot={} scheduledAt={} threshold={}",
                    draw.drawId(),
                    slot,
                    draw.scheduledAt(),
                    threshold);

                meterRegistry
                    .counter("draw_provisional_stuck_total", "slot", slot)
                    .increment();
            }
        } finally {
            binder.clear();
        }
    }

    private void validateCanRun() {
        if (!drawProps.getScheduler().isActive()) {
            throw new JobSkippedException("scheduler_disabled", "Draw scheduler disabled");
        }

        if (!drawProps.getWatchdog().isActive()) {
            throw new JobSkippedException("watchdog_disabled", "Draw watchdog disabled");
        }

        if (!batchGate.enabled(DRAW_WATCHDOG_PROVISIONAL, null)) {
            throw new JobSkippedException(
                "gate_disabled",
                "Draw provisional watchdog gate disabled");
        }
    }
}
