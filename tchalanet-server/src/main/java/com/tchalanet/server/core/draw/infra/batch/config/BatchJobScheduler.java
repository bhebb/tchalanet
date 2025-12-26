package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.infra.batch.results.fetch.DrawResultsJobStarter;
import com.tchalanet.server.core.draw.infra.batch.results.settle.DrawSettleJobStarter;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import java.time.Clock;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchJobScheduler {

  private final DrawResultsJobStarter drawResultsJobStarter;
  private final DrawSettleJobStarter drawSettleJobStarter;
  private final Clock clock;
  private final TenantReaderPort tenantReaderPort;

  // --------------------
  // FETCH (5 min after)
  // --------------------
  @Scheduled(cron = "0 35 14 * * *", zone = "America/New_York") // NY 14:30 -> 14:35
  public void fetchNyMidday() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startFetch(tenantId, "US_NY_NUM3_MID", 1, 300, false);
      startFetch(tenantId, "US_NY_NUM4_MID", 1, 300, false);
    }
  }

  @Scheduled(cron = "0 35 22 * * *", zone = "America/New_York") // NY 22:30 -> 22:35
  public void fetchNyEvening() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startFetch(tenantId, "US_NY_NUM3_EVE", 1, 400, false);
      startFetch(tenantId, "US_NY_NUM4_EVE", 1, 400, false);
    }
  }

  @Scheduled(cron = "0 35 13 * * *", zone = "America/New_York") // FL 13:30 -> 13:35
  public void fetchFloridaMidday() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startFetch(tenantId, "US_FL_NUM3_MID", 1, 400, false);
      startFetch(tenantId, "US_FL_NUM4_MID", 1, 400, false);
    }
  }

  @Scheduled(cron = "0 50 22 * * *", zone = "America/New_York") // FL 22:45 -> 22:50
  public void fetchFloridaEvening() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startFetch(tenantId, "US_FL_NUM3_EVE", 1, 500, false);
      startFetch(tenantId, "US_FL_NUM4_EVE", 1, 500, false);
    }
  }

  @Scheduled(cron = "0 20 23 * * *", zone = "America/New_York") // Florida Lotto 23:15 -> 23:20
  public void fetchFloridaLotto() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startFetch(tenantId, "US_FL_LOTTO_EVE", 1, 200, false);
    }
  }

  // --------------------
  // SETTLE (10 min after)
  // --------------------
  @Scheduled(cron = "0 40 14 * * *", zone = "America/New_York")
  public void settleNyMidday() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startSettle(tenantId, "NY", 1, 500, false);
    }
  }

  @Scheduled(cron = "0 40 22 * * *", zone = "America/New_York")
  public void settleNyEvening() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startSettle(tenantId, "NY", 1, 700, false);
    }
  }

  @Scheduled(cron = "0 40 13 * * *", zone = "America/New_York")
  public void settleFloridaMidday() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startSettle(tenantId, "FLORIDA", 1, 700, false);
    }
  }

  @Scheduled(cron = "0 55 22 * * *", zone = "America/New_York")
  public void settleFloridaEvening() {
    for (var tenantId : tenantReaderPort.listActiveTenantIds()) {
      startSettle(tenantId, "FLORIDA", 1, 900, false);
    }
  }

  // --------------------
  // helpers
  // --------------------
  private void startFetch(
      TenantId tenantId, String channelCode, int daysBack, int maxDraws, boolean dryRun) {
    var params = new java.util.HashMap<String, String>();
    long ts = java.time.Instant.now(clock).toEpochMilli();
    params.put("ts", Long.toString(ts));

    params.put("tenant_id", tenantId.toString());

    params.put("channel_code", channelCode);
    params.put("days_back", Integer.toString(daysBack));
    params.put("max_draws", Integer.toString(maxDraws));
    params.put("dry_run", Boolean.toString(dryRun));
    params.put("force", "false");

    String jobName = "fetch_draw_results";

    log.info(
        "batch.scheduler: tenantId={} job={} ts={} channelCode={} daysBack={} maxDraws={} dryRun={}",
        tenantId,
        jobName,
        ts,
        channelCode,
        daysBack,
        maxDraws,
        dryRun);

    drawResultsJobStarter.startFetchDrawResultsJob(params);
  }

  private void startSettle(
      TenantId tenantId, String channelCode, int daysBack, int maxDraws, boolean dryRun) {
    var params = new HashMap<String, String>();
    long ts = java.time.Instant.now(clock).toEpochMilli();
    params.put("ts", Long.toString(ts));
    params.put("tenant_id", tenantId.toString());
    if (channelCode != null) params.put("channel_code", channelCode);
    params.put("days_back", Integer.toString(daysBack));
    params.put("max_draws", Integer.toString(maxDraws));
    params.put("dry_run", Boolean.toString(dryRun));
    params.put("force", "false");

    String jobName = "settle_resulted_draws";
    log.info(
        "batch.scheduler: tenantId={} job={} ts={} channelCode={} daysBack={} maxDraws={} dryRun={}",
        tenantId,
        jobName,
        ts,
        channelCode,
        daysBack,
        maxDraws,
        dryRun);
    drawSettleJobStarter.startSettleDrawsJob(params);
  }
}
