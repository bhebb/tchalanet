package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.batch.annotation.BatchScheduledJob;
import com.tchalanet.server.common.batch.context.BatchTchContextBinder;
import com.tchalanet.server.common.batch.exception.BatchContextClearException;
import com.tchalanet.server.common.batch.exception.BatchPartialFailureException;
import com.tchalanet.server.common.batch.exception.BatchSkippedException;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import com.tchalanet.server.core.draw.infra.config.DrawSchedulerWindows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawLifeCycleTickScheduler {

    private static final boolean DEFAULT_DRY_RUN = false;

    private final TenantCatalog tenantCatalog;
    private final CommandBus commandBus;
    private final BatchGate batchGate;
    private final DrawSchedulerWindows windows;
    private final Clock clock;
    private final BatchTchContextBinder binder;
    private final DrawProperties drawProps;

    @BatchScheduledJob("draw:lifecycle:generate")
    @Scheduled(cron = "${tch.draw.lifecycle.generate_cron:0 0 5 * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_generate_next_7_days", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void generateNext7Days() {
        log.info("draw.generate.tick fired");

        validateGenerateCanRun();

        var now = clock.instant();

        var schedulerZone = drawProps.getScheduler().getWindows().getTimezone();
        var from = LocalDate.now(schedulerZone);

        int generationDays = Math.max(1, drawProps.getLifecycle().getGenerationDays());
        var to = from.plusDays(generationDays - 1L);

        var activeTenants = tenantCatalog.listActiveTenantIds();
        if (activeTenants.isEmpty()) {
            throw new BatchSkippedException("no_active_tenants", "No active tenants");
        }
        var failures = new ArrayList<TenantFailure>();

        for (TenantId tenantId : activeTenants) {
            var jp = jobParams(tenantId, "draw-generate", now);

            try {
                binder.bind(jp);

                commandBus.send(new GenerateDrawsForRangeCommand(
                    tenantId,
                    from,
                    to,
                    DEFAULT_DRY_RUN,
                    false,
                    null
                ));

            } catch (Exception ex) {
                failures.add(new TenantFailure(tenantId, ex));

                log.warn(
                    "draw.generate tenant failed tenantId={} from={} to={} err={}",
                    tenantId,
                    from,
                    to,
                    ex.getMessage(),
                    ex
                );
            } finally {
                var clearFailure = clearContext(tenantId);
                if (clearFailure != null) {
                    failures.add(new TenantFailure(tenantId, clearFailure));
                }
            }
        }

        if (!failures.isEmpty()) {
            throw new BatchPartialFailureException(
                "draw_generate_partial_failure",
                "Draw generate failed with " + failures.size() + " failures"
            );
        }
    }

    private void validateGenerateCanRun() {
        if (!drawProps.getScheduler().isActive()) {
            throw new BatchSkippedException("scheduler_disabled", "Draw scheduler disabled");
        }
        if (!drawProps.getLifecycle().isActive()) {
            throw new BatchSkippedException("lifecycle_disabled", "Draw lifecycle disabled");
        }

        if (!batchGate.enabled(BatchJobKeys.DRAW_GENERATE, null)) {
            throw new BatchSkippedException("gate_disabled", "Draw generate gate disabled");
        }
    }

    @BatchScheduledJob("draw:lifecycle:open_close")
    @Scheduled(cron = "${tch.draw.lifecycle.open_close_cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_open_close_windowed", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void openCloseWindowed() {
        log.info("draw.open_close.tick fired");
        var now = clock.instant();

        validateOpenCloseCanRun();

        var windowProps = drawProps.getScheduler().getWindows();
        var localNow = now.atZone(windowProps.getTimezone()).toLocalTime();

        boolean windowsEnabled = windowProps.isEnabled();

        boolean openWindowOk = !windowsEnabled || windows.isInOpenDrawsWindow(localNow);
        boolean closeWindowOk = !windowsEnabled || windows.isInCloseDrawsWindow(localNow);

        boolean openGateGlobalOk = batchGate.enabled(BatchJobKeys.DRAW_OPEN, null);
        boolean closeGateGlobalOk = batchGate.enabled(BatchJobKeys.DRAW_CLOSE, null);

        var canOpen = openWindowOk && openGateGlobalOk;
        var canClose = closeWindowOk && closeGateGlobalOk;

        if (!canOpen && !canClose) {
            throw new BatchSkippedException(
                "outside_window_or_gate_disabled",
                "Outside open/close windows or gates disabled"
            );
        }

        var lc = drawProps.getLifecycle();
        var activeTenants = tenantCatalog.listActiveTenantIds();
        if (activeTenants.isEmpty()) {
            throw new BatchSkippedException("no_active_tenants", "No active tenants");
        }
        var failures = new ArrayList<TenantFailure>();

        for (TenantId tenantId : activeTenants) {
            var jp = jobParams(tenantId, "draw-open-close", now);

            try {
                binder.bind(jp);

                if (canOpen) {
                    commandBus.send(new OpenDueDrawsCommand(
                        now,
                        lc.getBatchSize(),
                        lc.getLookaheadHours(),
                        lc.getLagHours(),
                        false
                    ));
                }

                if (canClose) {
                    commandBus.send(new CloseDueDrawsCommand(
                        now,
                        lc.getBatchSize(),
                        false
                    ));
                }

            } catch (Exception ex) {
                failures.add(new TenantFailure(tenantId, ex));

                log.warn(
                    "draw.open_close tenant failed tenantId={} canOpen={} canClose={} err={}",
                    tenantId,
                    canOpen,
                    canClose,
                    ex.getMessage(),
                    ex
                );
            } finally {
                var clearFailure = clearContext(tenantId);
                if (clearFailure != null) {
                    failures.add(new TenantFailure(tenantId, clearFailure));
                }
            }
        }

        if (!failures.isEmpty()) {
            throw new BatchPartialFailureException(
                "draw_open_close_partial_failure",
                "Draw open/close failed for " + failures.size() + " failures"
            );
        }
    }


    private void validateOpenCloseCanRun() {
        if (!drawProps.getScheduler().isActive()) {
            throw new BatchSkippedException("scheduler_disabled", "Draw scheduler disabled");
        }
        if (!drawProps.getLifecycle().isActive()) {
            throw new BatchSkippedException("lifecycle_disabled", "Draw lifecycle disabled");
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

    private Exception clearContext(TenantId tenantId) {
        try {
            binder.clear();
            return null;
        } catch (Exception ex) {
            log.error(
                "draw.lifecycle failed to clear context tenantId={} err={}",
                tenantId,
                ex.getMessage(),
                ex
            );
            return new BatchContextClearException("context_clear_failed", ex);
        }
    }

    private record TenantFailure(TenantId tenantId, Exception cause) {
    }
}
