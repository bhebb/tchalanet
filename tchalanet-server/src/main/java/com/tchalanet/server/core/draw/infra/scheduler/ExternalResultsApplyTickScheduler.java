package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
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
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import com.tchalanet.server.core.draw.infra.config.DrawSchedulerWindows;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.draw.results.scheduler",
    name = "active",
    havingValue = "true",
    matchIfMissing = false
)
public class ExternalResultsApplyTickScheduler {

    private static final boolean DEFAULT_DRY_RUN = false;
    private static final boolean DEFAULT_FORCE = false;

    private final CommandBus commandBus;
    private final BatchGate gate;
    private final TenantCatalog tenantCatalog;
    private final ResultSlotCatalog resultSlotCatalog;
    private final Clock clock;
    private final BatchTchContextBinder binder;
    private final DrawProperties drawProps;
    private final DrawResultsProperties resultsProps;
    private final DrawSchedulerWindows windows;

    @BatchScheduledJob("draw:results:apply")
    @Scheduled(cron = "${tch.draw.results.scheduler.cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_results_apply_tick", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void tickApply() {
        log.info("draw.results.apply.tick fired");

        var now = clock.instant();

        var windowProps = drawProps.getScheduler().getWindows();
        var localNow = now.atZone(windowProps.getTimezone()).toLocalTime();

        validateApplyCanRun(localNow);

        var tenants = tenantCatalog.listActiveTenantIds();
        if (tenants.isEmpty()) {
            throw new BatchSkippedException("no_active_tenants", "No active tenants");
        }

        var slots =
            resultSlotCatalog.listActive().stream()
                .filter(s -> s.timezone() != null && s.drawTime() != null)
                .toList();

        if (slots.isEmpty()) {
            throw new BatchSkippedException("no_active_result_slots", "No active result slots");
        }

        var failures = new ArrayList<TenantFailure>();

        for (TenantId tenantId : tenants) {
            var requestId = "draw-results-apply-" + now.toEpochMilli() + "-" + UUID.randomUUID();

            var jp =
                new JobParametersBuilder()
                    .addString(BatchParamKeys.TENANT_ID, tenantId.value().toString())
                    .addString(BatchParamKeys.REQUEST_ID, requestId)
                    .addString(BatchParamKeys.ACTOR, "scheduler")
                    .toJobParameters();

            try {
                binder.bind(jp);

                for (var slot : slots) {
                    var baseDate = LocalDate.now(clock.withZone(slot.timezone()));

                    commandBus.send(
                        new ApplyExternalResultsWindowCommand(
                            tenantId,
                            baseDate,
                            resultsProps.getDefaults().getManualDaysBack(),
                            List.of(slot.slotKey()),
                            DEFAULT_DRY_RUN,
                            DEFAULT_FORCE,
                            resultsProps.getDefaults().getManualMaxSlots(),
                            null));
                }

                log.info(
                    "draw.results.apply.tick tenant completed tenantId={} requestId={} slots={}",
                    tenantId,
                    requestId,
                    slots.size());

            } catch (Exception ex) {
                failures.add(new TenantFailure(tenantId, ex));

                log.warn(
                    "draw.results.apply.tick tenant failed tenantId={} requestId={} err={}",
                    tenantId,
                    requestId,
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
            throw new BatchPartialFailureException(
                "draw_results_apply_partial_failure",
                "Draw results apply failed with " + failures.size() + " failure(s)");
        }
    }

    private void validateApplyCanRun(LocalTime localNow) {
        if (!drawProps.getScheduler().isActive()) {
            throw new BatchSkippedException("scheduler_disabled", "Draw scheduler disabled");
        }

        if (!resultsProps.isActive()) {
            throw new BatchSkippedException("results_disabled", "Draw results disabled");
        }

        if (!resultsProps.getScheduler().isActive()) {
            throw new BatchSkippedException(
                "results_scheduler_disabled",
                "Draw results scheduler disabled");
        }

        if (!gate.enabled(BatchJobKeys.RESULTS_EXTERNAL_APPLY, null)) {
            throw new BatchSkippedException("gate_disabled", "Apply gate disabled");
        }

        var windowProps = drawProps.getScheduler().getWindows();
        boolean windowsEnabled = windowProps.isEnabled();

        if (windowsEnabled && !windows.isInApplyResultsWindow(localNow)) {
            throw new BatchSkippedException("outside_window", "Outside results apply window");
        }
    }

    private Exception clearContext(TenantId tenantId) {
        try {
            binder.clear();
            return null;
        } catch (Exception ex) {
            log.error(
                "draw.results.apply failed to clear context tenantId={} err={}",
                tenantId,
                ex.getMessage(),
                ex);
            return new BatchContextClearException("context_clear_failed", ex);
        }
    }

    private record TenantFailure(TenantId tenantId, Exception cause) {}
}
