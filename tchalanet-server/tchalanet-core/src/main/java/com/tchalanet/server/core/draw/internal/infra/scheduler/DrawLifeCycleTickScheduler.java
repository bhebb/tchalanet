package com.tchalanet.server.core.draw.internal.infra.scheduler;

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
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenTodayDrawsCommand;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawLifeCycleTickScheduler {

    private static final boolean DEFAULT_DRY_RUN = false;

    private final TenantCatalog tenantCatalog;
    private final CommandBus commandBus;
    private final BatchGate batchGate;
    private final Clock clock;
    private final BatchTchContextBinder binder;
    private final DrawProperties drawProps;
    private final AtomicBoolean configLogged = new AtomicBoolean(false);

    @BatchScheduledJob("draw:lifecycle:generate")
    @Scheduled(cron = "${tch.draw.scheduler.generate.cron:0 0 5 * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_generate_next_7_days", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void generateNext7Days() {
        log.info("draw.generate.tick fired");
        logEffectiveConfigOnce();

        validateGenerateCanRun();

        var now = clock.instant();

        var schedulerZone = drawProps.getScheduler().getProcessing().getTimezone();
        var from = LocalDate.now(schedulerZone);

        int generationDays = Math.max(1, drawProps.getScheduler().getGenerate().getDaysAhead());
        var to = from.plusDays(generationDays - 1L);

        var activeTenants = tenantCatalog.listActiveTenantIds();
        if (activeTenants.isEmpty()) {
            throw new BatchSkippedException("no_active_tenants", "No active tenants");
        }
        var failures = new ArrayList<TenantFailure>();

        int maxTenants = Math.max(1, drawProps.getScheduler().getGenerate().getMaxTenantsPerRun());

        for (TenantId tenantId : activeTenants.stream().limit(maxTenants).toList()) {

            try {
                binder.bind(jobParams(tenantId, "draw-generate", now));

                commandBus.execute(new GenerateDrawsForRangeCommand(
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
        if (!drawProps.getScheduler().getGenerate().isActive()) {
            throw new BatchSkippedException("generate_disabled", "Draw generate scheduler disabled");
        }

        if (!batchGate.enabled(BatchJobKeys.DRAW_GENERATE, null)) {
            throw new BatchSkippedException("gate_disabled", "Draw generate gate disabled");
        }
    }

    @BatchScheduledJob("draw:lifecycle:open_today")
    @Scheduled(cron = "${tch.draw.scheduler.open-today.cron:0 */5 4-10 * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_open_today", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void openToday() {
        log.info("draw.open_today.tick fired");
        logEffectiveConfigOnce();
        var now = clock.instant();

        validateOpenTodayCanRun();

        var maxItems = Math.max(1, drawProps.getScheduler().getOpenToday().getMaxItemsPerRun());
        var defaultSalesOpenTime = drawProps.getScheduler().getOpenToday().getDefaultSalesOpenTime();
        var activeTenants = tenantCatalog.listActiveTenantIds();
        if (activeTenants.isEmpty()) {
            throw new BatchSkippedException("no_active_tenants", "No active tenants");
        }
        var failures = new ArrayList<TenantFailure>();

        for (TenantId tenantId : activeTenants) {
            var jp = jobParams(tenantId, "draw-open-close", now);

            try {
                binder.bind(jp);

                commandBus.execute(new OpenTodayDrawsCommand(
                        now,
                        null,
                        defaultSalesOpenTime,
                        maxItems,
                        false));

            } catch (Exception ex) {
                failures.add(new TenantFailure(tenantId, ex));

                log.warn(
                    "draw.open_today tenant failed tenantId={} err={}",
                    tenantId,
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
                "draw_open_today_partial_failure",
                "Draw open today failed for " + failures.size() + " failures"
            );
        }
    }


    private void validateOpenTodayCanRun() {
        if (!drawProps.getScheduler().isActive()) {
            throw new BatchSkippedException("scheduler_disabled", "Draw scheduler disabled");
        }
        if (!drawProps.getScheduler().getOpenToday().isActive()) {
            throw new BatchSkippedException("open_today_disabled", "Draw open-today scheduler disabled");
        }
        if (!batchGate.enabled(BatchJobKeys.DRAW_OPEN, null)) {
            throw new BatchSkippedException("gate_disabled", "Draw open gate disabled");
        }
    }

    private void logEffectiveConfigOnce() {
        if (!configLogged.compareAndSet(false, true)) {
            return;
        }
        var scheduler = drawProps.getScheduler();
        log.info(
            "draw.scheduler.config active={} generate.active={} generate.cron={} generate.daysAhead={} openToday.active={} openToday.cron={} openToday.defaultSalesOpenTime={} processing.active={} processing.cron={} processing.timezone={}",
            scheduler.isActive(),
            scheduler.getGenerate().isActive(),
            scheduler.getGenerate().getCron(),
            scheduler.getGenerate().getDaysAhead(),
            scheduler.getOpenToday().isActive(),
            scheduler.getOpenToday().getCron(),
            scheduler.getOpenToday().getDefaultSalesOpenTime(),
            scheduler.getProcessing().isActive(),
            scheduler.getProcessing().getCron(),
            scheduler.getProcessing().getTimezone());
    }


    private JobParameters jobParams(TenantId tenantId, String kind, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(now, "now is required");

        String safeKind = kind == null || kind.isBlank() ? "batch" : kind.trim();
        String requestId = safeKind + "-" + now.toEpochMilli() + "-" + UUID.randomUUID();

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
