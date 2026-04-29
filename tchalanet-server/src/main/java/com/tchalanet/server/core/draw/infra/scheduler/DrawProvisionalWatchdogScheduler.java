package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.core.draw.api.DrawReaderPort;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
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

  private final DrawReaderPort drawReader;
  private final MeterRegistry meterRegistry;
  private final BatchGate batchGate;
  private final Clock clock;
  private final DrawProperties drawProps;

  @Scheduled(cron = "${tch.draw.watchdog.provisional_cron:0 */15 * * * *}", zone = "UTC")
  @SchedulerLock(name = "draw_provisional_watchdog", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
  public void checkProvisionalStuck() {
    if (!batchGate.enabled(BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL, null)) {
      log.debug("batch.skip jobKey={} reason=disabled", BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL);
      return;
    }

    log.debug("Checking for draws stuck with PROVISIONAL results...");

    var stuckDraws = drawReader.findResultedWithProvisionalOlderThan(
        Duration.ofMinutes(drawProps.getWatchdog().getProvisionalStuckMinutes()));

    if (!stuckDraws.isEmpty()) {
      stuckDraws.forEach(draw -> {
        log.warn("Draw {} stuck in RESULTED with PROVISIONAL result, slot={}, scheduledAt={}",
            draw.id(), draw.channelCode(), draw.scheduledAt());

        meterRegistry.counter("draw_provisional_stuck_total",
            "slot", draw.channelCode() != null ? draw.channelCode() : "unknown"
        ).increment();
      });
    }
  }
}
