package com.tchalanet.server.core.draw.internal.infra.scheduler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.job.exception.JobContextClearException;
import com.tchalanet.server.common.job.exception.JobPartialFailureException;
import com.tchalanet.server.common.job.exception.JobSkippedException;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
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
  private final TenantCatalog tenantCatalog;
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

    var threshold = Duration.ofMinutes(drawProps.getWatchdog().getProvisionalStuckMinutes());
    var activeTenants = tenantCatalog.listActiveTenantIds();
    if (activeTenants.isEmpty()) {
      throw new JobSkippedException("no_active_tenants", "No active tenants");
    }

    var failures = new ArrayList<TenantFailure>();
    for (TenantId tenantId : activeTenants) {
      try {
        binder.bindTenant(tenantId, "draw-watchdog-scheduler");
        var stuckDraws = drawReader.findResultedWithProvisionalOlderThan(threshold);

        if (stuckDraws.isEmpty()) {
          log.debug(
              "draw.watchdog.provisional no stuck draws tenantId={} threshold={}", tenantId, threshold);
          continue;
        }

        for (var draw : stuckDraws) {
          var slot = draw.drawChannelCode() != null ? draw.drawChannelCode() : "unknown";
          log.warn(
              "draw.watchdog.provisional stuck drawId={} tenantId={} slot={} scheduledAt={} threshold={}",
              draw.drawId(),
              tenantId,
              slot,
              draw.scheduledAt(),
              threshold);
          meterRegistry.counter("draw_provisional_stuck_total", "slot", slot).increment();
        }
      } catch (Exception ex) {
        failures.add(new TenantFailure(tenantId, ex));
        log.warn(
            "draw.watchdog.provisional tenant failed tenantId={} threshold={} err={}",
            tenantId,
            threshold,
            ex.getMessage(),
            ex);
      } finally {
        var clearFailure = clearContext(tenantId);
        if (clearFailure != null) {
          failures.add(new TenantFailure(tenantId, clearFailure));
        }
      }
    }

    if (!failures.isEmpty()) {
      throw new JobPartialFailureException(
          "draw_watchdog_provisional_partial_failure",
          "Draw provisional watchdog failed for " + failures.size() + " failures");
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
      throw new JobSkippedException("gate_disabled", "Draw provisional watchdog gate disabled");
    }
  }

  private Exception clearContext(TenantId tenantId) {
    try {
      binder.clear();
      return null;
    } catch (Exception ex) {
      log.error(
          "draw.watchdog.provisional failed to clear context tenantId={} err={}",
          tenantId,
          ex.getMessage(),
          ex);
      return new JobContextClearException("context_clear_failed", ex);
    }
  }

  private record TenantFailure(TenantId tenantId, Exception cause) {}
}
