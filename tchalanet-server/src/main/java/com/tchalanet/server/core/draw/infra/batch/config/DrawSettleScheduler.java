package com.tchalanet.server.core.draw.infra.batch.config;

import static com.tchalanet.server.common.time.DefaultTimeZone.AMERICA_NEW_YORK;

import com.tchalanet.server.common.batch.BatchGate;
import com.tchalanet.server.common.config.batch.BatchWindowsConfig;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.infra.batch.results.settle.DrawSettleJobStarter;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import java.time.Clock;
import java.time.LocalTime;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawSettleScheduler {

  private final DrawSettleJobStarter jobStarter;
  private final Clock clock;
  private final TenantReaderPort tenantReaderPort;

  private final BatchGate gate;
  private final BatchWindowsConfig windows;

  //  @Scheduled(cron = "0 */5 * * * *", zone = "America/New_York")
  public void tick() {
    if (!gate.enabled("draw.settle.enabled", null)) return;

    var nowLocal = LocalTime.now(AMERICA_NEW_YORK);
    if (!windows.isInSettleDrawsWindow(nowLocal)) return;

    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      // per-tenant override possible later
      if (!gate.enabled("draw.settle.enabled", tenantId)) continue;

      // settle NY + FL in same tick (simple)
      startSettle(tenantId, "NY", 1, 700, false, false);
      startSettle(tenantId, "FL", 1, 900, false, false);
    }
  }

  private void startSettle(
      TenantId tenantId,
      String provider,
      int daysBack,
      int maxDraws,
      boolean force,
      boolean dryRun) {
    // provider-level switch
    if (!gate.enabled("draw.settle.provider." + provider.toLowerCase() + ".enabled", tenantId))
      return;

    long ts = java.time.Instant.now(clock).toEpochMilli();

    var params = new HashMap<String, String>();
    params.put("ts", Long.toString(ts));
    params.put("tenant_id", tenantId.toString());
    params.put("provider", provider);
    params.put("days_back", Integer.toString(daysBack));
    params.put("max_draws", Integer.toString(maxDraws));
    params.put("force", Boolean.toString(force));
    params.put("dry_run", Boolean.toString(dryRun));

    log.info(
        "batch.scheduler: job=settle_draws tenantId={} provider={} daysBack={} maxDraws={} force={} dryRun={} ts={}",
        tenantId,
        provider,
        daysBack,
        maxDraws,
        force,
        dryRun,
        ts);

    jobStarter.startSettleDrawsJob(params);
  }
}
