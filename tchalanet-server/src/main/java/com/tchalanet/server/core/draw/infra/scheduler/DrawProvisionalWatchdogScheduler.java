package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.core.draw.api.DrawReaderPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @Scheduled(cron = "${tch.draw.watchdog.provisional_cron:0 */15 * * * *}")
  public void checkProvisionalStuck() {
    log.debug("Checking for draws stuck with PROVISIONAL results...");

    var stuckDraws = drawReader.findResultedWithProvisionalOlderThan(Duration.ofMinutes(30));

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
