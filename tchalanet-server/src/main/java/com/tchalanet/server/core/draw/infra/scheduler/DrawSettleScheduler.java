package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.batch.launch.BatchJobStarter;
import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.config.batch.BatchWindowsConfig;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.UUID;

import static com.tchalanet.server.common.batch.key.BatchJobKeys.DRAW_SETTLE;
import static com.tchalanet.server.common.time.DefaultTimeZone.AMERICA_NEW_YORK;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawSettleScheduler {


    // optionnel: provider-level switches
    private static final JobKey SWITCH_NY = JobKey.of("draw:settle:provider:ny");
    private static final JobKey SWITCH_FL = JobKey.of("draw:settle:provider:fl");

    private final BatchJobStarter batchJobStarter;
    private final Clock clock;
    private final TenantCatalog tenantCatalog;
    private final BatchGate gate;
    private final BatchWindowsConfig windows;

    @Scheduled(cron = "0 */5 * * * *", zone = "America/New_York")
    public void tick() {
        // global switch
        if (!gate.enabled(DRAW_SETTLE, null)) return;

        var nowLocal = LocalTime.now(AMERICA_NEW_YORK);
        if (!windows.isInSettleDrawsWindow(nowLocal)) return;

        Instant now = Instant.now(clock);
        var tenantIds = tenantCatalog.listActiveTenantIds();
        for (TenantId tenantId : tenantIds) {
            if (!gate.enabled(DRAW_SETTLE, tenantId)) continue;

            startForProvider(now, tenantId, "NY", SWITCH_NY, 1, 700, false, false);
            startForProvider(now, tenantId, "FL", SWITCH_FL, 1, 900, false, false);
        }
    }

    private void startForProvider(
        Instant now,
        TenantId tenantId,
        String provider,
        JobKey providerSwitch,
        int daysBack,
        int maxDraws,
        boolean force,
        boolean dryRun
    ) {
        // provider-level switch (optional)
        if (!gate.enabled(providerSwitch, tenantId)) return;

        var params = new HashMap<String, String>();
        params.put(BatchParamKeys.TENANT_ID, tenantId.toString());

        // request tracing
        params.put(BatchParamKeys.REQUEST_ID, "tick-" + now.toEpochMilli() + "-" + UUID.randomUUID());
        params.put(BatchParamKeys.ACTOR, "scheduler");
        params.put(BatchParamKeys.DRY_RUN, Boolean.toString(dryRun));

        // settle-specific params (snake_case)
        params.put(BatchParamKeys.PROVIDER, provider);
        params.put(BatchParamKeys.DAYS_BACK, Integer.toString(daysBack));
        params.put(BatchParamKeys.MAX_DRAWS, Integer.toString(maxDraws));
        params.put(BatchParamKeys.FORCE, Boolean.toString(force));

        // NOTE: do NOT set ts here unless you really want deterministic ts
        // BatchJobStarter will add identifying ts if missing.

        var exec = batchJobStarter.start(DRAW_SETTLE, params);

        log.info("batch.scheduler.started jobKey={} tenantId={} provider={} executionId={}",
            DRAW_SETTLE, tenantId, provider, exec.getId());
    }
}
