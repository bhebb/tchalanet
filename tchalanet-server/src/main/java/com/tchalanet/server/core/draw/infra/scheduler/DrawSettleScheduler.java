package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.batch.annotation.BatchScheduledJob;
import com.tchalanet.server.common.batch.exception.BatchSkippedException;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.launch.BatchJobStarter;
import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import com.tchalanet.server.core.draw.infra.config.DrawSchedulerWindows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.UUID;

import static com.tchalanet.server.common.batch.key.BatchJobKeys.DRAW_SETTLE;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawSettleScheduler {

    private final BatchJobStarter batchJobStarter;
    private final Clock clock;
    private final TenantCatalog tenantCatalog;
    private final BatchGate gate;
    private final DrawSchedulerWindows windows;
    private final DrawProperties drawProperties;

    @BatchScheduledJob("draw:lifecycle:settle")
    @Scheduled(cron = "${tch.draw.settlement.cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_settle_tick", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void tick() {
        log.info("draw.settlement.tick fired");
        var now = clock.instant();
        var localNow = now.atZone(drawProperties.getScheduler().getWindows().getTimezone()).toLocalTime();

        validateSettleCanRun(localNow);

        for (TenantId tenantId : tenantCatalog.listActiveTenantIds()) {
            if (!gate.enabled(DRAW_SETTLE, tenantId)) continue;

            var params = new HashMap<String, String>();
            params.put(BatchParamKeys.TENANT_ID, tenantId.value().toString());
            params.put(BatchParamKeys.REQUEST_ID, "draw-settle-" + now.toEpochMilli() + "-" + UUID.randomUUID());
            params.put(BatchParamKeys.ACTOR, "scheduler");
            params.put(BatchParamKeys.DAYS_BACK, Integer.toString(drawProperties.getSettlement().getDaysBack()));
            params.put(BatchParamKeys.MAX_DRAWS, Integer.toString(drawProperties.getSettlement().getMaxDrawsPerTenant()));
            params.put(BatchParamKeys.DRY_RUN, "false");
            params.put(BatchParamKeys.FORCE, "false");

            var exec = batchJobStarter.start(DRAW_SETTLE, params);

            log.info("batch.scheduler.started jobKey={} tenantId={} executionId={}",
                DRAW_SETTLE, tenantId, exec.getId());
        }
    }


    private void validateSettleCanRun(LocalTime localNow) {
        if (!drawProperties.getScheduler().isActive()) {
            throw new BatchSkippedException("scheduler_disabled", "Draw scheduler disabled");
        }
        if (!drawProperties.getSettlement().isActive()) {
            throw new BatchSkippedException("settlement_disabled", "Draw settlement disabled");
        }
        if (!gate.enabled(DRAW_SETTLE, null)) {
            throw new BatchSkippedException("gate_disabled", "Draw settle gate disabled");
        }
        if (!windows.isInSettleDrawsWindow(localNow)) {
            throw new BatchSkippedException("outside_window", "Outside settle window");
        }
    }
}
