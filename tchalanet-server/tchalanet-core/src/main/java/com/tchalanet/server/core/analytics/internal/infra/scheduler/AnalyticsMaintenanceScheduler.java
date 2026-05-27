package com.tchalanet.server.core.analytics.internal.infra.scheduler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.exception.JobSkippedException;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.core.analytics.api.command.PurgeAnalyticsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Thin scheduler for analytics maintenance.
 *
 * <p>Delegates all logic to command handlers — no business logic here.
 * Guarded by {@link BatchGate} so it only runs when the gate is enabled.
 *
 * <p>Default schedule: 03:15 UTC daily (off-peak, after most tenant draws settle).
 * Override via {@code tch.analytics.purge-cron}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsMaintenanceScheduler {

  private static final JobKey ANALYTICS_PURGE_GATE = JobKey.of("analytics:purge:enabled");

  private final CommandBus commandBus;
  private final BatchGate  gate;

  @Scheduled(cron = "${tch.analytics.purge-cron:0 15 3 * * *}")
  @TchJob("analytics:purge")
  public void purge() {
    if (!gate.enabled(ANALYTICS_PURGE_GATE, null)) {
      throw new JobSkippedException("gate_disabled", "analytics purge gate disabled");
    }
    log.info("analytics maintenance: starting scheduled purge");
    commandBus.execute(PurgeAnalyticsCommand.scheduled());
  }
}
