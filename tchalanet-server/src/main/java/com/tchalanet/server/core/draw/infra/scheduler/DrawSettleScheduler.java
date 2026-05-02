package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.batch.launch.BatchJobStarter;
import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.config.batch.BatchWindowsConfig;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import static com.tchalanet.server.common.batch.key.BatchJobKeys.DRAW_SETTLE;
import static com.tchalanet.server.common.time.DefaultTimeZone.AMERICA_NEW_YORK;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawSettleScheduler {

    private final BatchJobStarter batchJobStarter;
    private final Clock clock;
    private final TenantCatalog tenantCatalog;
    private final BatchGate gate;
    private final BatchWindowsConfig windows;
    private final DrawProperties drawProperties;

    @Scheduled(cron = "${tch.draw.settle.cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_settle_tick", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void tick() {
        log.info("draw.settle.tick fired");
        // global switch
        if (!gate.enabled(DRAW_SETTLE, null)) return;

        var nowLocal = LocalTime.now(AMERICA_NEW_YORK);
        if (!windows.isInSettleDrawsWindow(nowLocal)) return;

        Instant now = Instant.now(clock);
        var tenantIds = tenantCatalog.listActiveTenantIds();
        var settle = drawProperties.getSettle();
        for (TenantId tenantId : tenantIds) {
            if (!gate.enabled(DRAW_SETTLE, tenantId)) continue;

            for (String provider : settle.getProviders()) {
                var normalizedProvider = normalizeProvider(provider);
                if (normalizedProvider.isBlank()) continue;
                int maxDraws = settle.getMaxDrawsByProvider()
                    .getOrDefault(normalizedProvider, settle.getDefaultMaxDraws());
                startForProvider(
                    now,
                    tenantId,
                    normalizedProvider,
                    JobKey.of("draw:settle:provider:" + normalizedProvider.toLowerCase(Locale.ROOT)),
                    settle.getDaysBack(),
                    maxDraws,
                    false,
                    false);
            }
        }
    }

    private static String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
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
