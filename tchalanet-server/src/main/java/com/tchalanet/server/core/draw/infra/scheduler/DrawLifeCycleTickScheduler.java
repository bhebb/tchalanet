package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.common.batch.context.BatchTchContextBinder;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.config.batch.BatchWindowsConfig;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.port.out.TenantDrawCalendarQueryPort;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.tchalanet.server.common.batch.key.BatchJobKeys.DRAW_GENERATE;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawLifeCycleTickScheduler {

    private static final boolean DEFAULT_DRY_RUN = false;
    private static final ZoneId OPS_ZONE = ZoneId.of("America/New_York");

    private final TenantDrawCalendarQueryPort tenantPort;
    private final CommandBus commandBus;
    private final BatchGate batchGate;
    private final BatchWindowsConfig windows;
    private final Clock clock;
    private final BatchTchContextBinder binder;
    private final DrawProperties drawProps;

    @Scheduled(cron = "${tch.draw.lifecycle.generate_cron:0 0 5 * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_generate_next_7_days", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void generateNext7Days() {
        if (!batchGate.enabled(DRAW_GENERATE, null)) {
            log.info("batch.skip jobKey={} reason=disabled", DRAW_GENERATE);
            return;
        }

        Instant now = Instant.now(clock);
        LocalDate from = LocalDate.now(clock);
        LocalDate to = from.plusDays(drawProps.getGeneration().getDays());

        for (TenantId tenantId : tenantPort.listActiveTenantIdsForDrawCalendar()) {
            JobParameters jp = jobParams(tenantId, "draw-generate", now);

            try {
                binder.bind(jp);
                commandBus.send(new GenerateDrawsForRangeCommand(tenantId, from, to, DEFAULT_DRY_RUN, false, null));
            } catch (Exception e) {
                log.warn("draw.generate failed tenantId={} from={} to={} err={}", tenantId, from, to, e.toString());
            } finally {
                safeClear(tenantId);
            }
        }
    }

    @Scheduled(cron = "${tch.draw.lifecycle.open_cron:0 */30 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_open_windowed", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void openWindowed() {
        if (!batchGate.enabled(BatchJobKeys.DRAW_OPEN, null)) return;

        Instant now = Instant.now(clock);
        var nowOps = now.atZone(OPS_ZONE).toLocalTime();
        if (!windows.isInOpenDrawsWindow(nowOps)) return;

        var lc = drawProps.getLifecycle();

        for (TenantId tenantId : tenantPort.listActiveTenantIdsForDrawCalendar()) {
            JobParameters jp = jobParams(tenantId, "draw-open", now);

            try {
                binder.bind(jp);
                commandBus.send(new OpenDueDrawsCommand(now, lc.getBatchSize(), lc.getLookaheadHours(), lc.getLagHours(), false));
            } catch (Exception e) {
                log.warn("draw.open failed tenantId={} err={}", tenantId, e.toString());
            } finally {
                safeClear(tenantId);
            }
        }
    }

    @Scheduled(cron = "${tch.draw.lifecycle.close_cron:0 */15 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_close_windowed", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void closeWindowed() {
        if (!batchGate.enabled(BatchJobKeys.DRAW_CLOSE, null)) return;

        Instant now = Instant.now(clock);
        var nowOps = now.atZone(OPS_ZONE).toLocalTime();
        if (!windows.isInCloseDrawsWindow(nowOps)) return;

        var lc = drawProps.getLifecycle();

        for (TenantId tenantId : tenantPort.listActiveTenantIdsForDrawCalendar()) {
            JobParameters jp = jobParams(tenantId, "draw-close", now);

            try {
                binder.bind(jp);
                commandBus.send(new CloseDueDrawsCommand(now, lc.getBatchSize(), false));
            } catch (Exception e) {
                log.warn("draw.close failed tenantId={} err={}", tenantId, e.toString());
            } finally {
                safeClear(tenantId);
            }
        }
    }

    private JobParameters jobParams(TenantId tenantId, String kind, Instant now) {
        String requestId = kind + "-" + now.toEpochMilli() + "-" + UUID.randomUUID();
        return new JobParametersBuilder()
            .addString(BatchParamKeys.TENANT_ID, tenantId.value().toString())
            .addString(BatchParamKeys.REQUEST_ID, requestId)
            .addString(BatchParamKeys.ACTOR, "scheduler")
            .toJobParameters();
    }

    private void safeClear(TenantId tenantId) {
        try {
            binder.clear();
        } catch (Exception ex) {
            log.warn("draw.lifecycle failed to clear context tenantId={} err={}", tenantId, ex.toString());
        }
    }
}
