package com.tchalanet.server.core.draw.internal.infra.scheduler;

import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.exception.JobPartialFailureException;
import com.tchalanet.server.common.job.exception.JobSkippedException;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawLifeCycleTickScheduler {

    private static final boolean DEFAULT_DRY_RUN = false;
    private static final boolean DEFAULT_FORCE = false;
    private static final JobKey DRAW_GENERATE = JobKey.of("draw:lifecycle:generate");
    private static final JobKey DRAW_OPEN = JobKey.of("draw:lifecycle:open");
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String DAYS_AHEAD = "days_ahead";
    private static final String MAX_ITEMS = "max_items";

    private final TenantPreContextLookupApi tenantRegistry;
    private final BatchJobStarter batchJobStarter;
    private final BatchGate batchGate;
    private final TchTimeProvider timeProvider;
    private final DrawProperties drawProps;
    private final AtomicBoolean configLogged = new AtomicBoolean(false);

    @TchJob("draw:lifecycle:generate")
    @Scheduled(cron = "${tch.draw.scheduler.generate.cron:0 0 5 * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_generate_next_7_days", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void generateNext7Days() {
        log.info("draw.generate.tick fired");
        logEffectiveConfigOnce();

        validateGenerateCanRun();

        var schedulerZone = drawProps.getScheduler().getProcessing().getTimezone();
        var from = timeProvider.today(schedulerZone);

        int generationDays = Math.max(1, drawProps.getScheduler().getGenerate().getDaysAhead());
        var to = from.plusDays(generationDays - 1L);

        var activeTenants = tenantRegistry.listActiveTenantIds();
        if (activeTenants.isEmpty()) {
            throw new JobSkippedException("no_active_tenants", "No active tenants");
        }
        var failures = new ArrayList<TenantFailure>();

        int maxTenants = Math.max(1, drawProps.getScheduler().getGenerate().getMaxTenantsPerRun());

        for (TenantId tenantId : activeTenants.stream().limit(maxTenants).toList()) {

            try {
                var execution = batchJobStarter.start(DRAW_GENERATE, generateParamsFor(tenantId, from, to, generationDays));
                log.info(
                    "draw.generate job started tenantId={} executionId={} from={} to={}",
                    tenantId,
                    execution.jobExecutionId(),
                    from,
                    to);
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
            }
        }

        if (!failures.isEmpty()) {
            throw new JobPartialFailureException(
                "draw_generate_partial_failure",
                "Draw generate failed with " + failures.size() + " failures"
            );
        }
    }

    private void validateGenerateCanRun() {
        if (!drawProps.getScheduler().isActive()) {
            throw new JobSkippedException("scheduler_disabled", "Draw scheduler disabled");
        }
        if (!drawProps.getScheduler().getGenerate().isActive()) {
            throw new JobSkippedException("generate_disabled", "Draw generate scheduler disabled");
        }

        if (!batchGate.enabled(DRAW_GENERATE, null)) {
            throw new JobSkippedException("gate_disabled", "Draw generate gate disabled");
        }
    }

    @TchJob("draw:lifecycle:open_today")
    @Scheduled(cron = "${tch.draw.scheduler.open-today.cron:0 */5 4-10 * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_open_today", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void openToday() {
        log.info("draw.open_today.tick fired");
        logEffectiveConfigOnce();
        var now = timeProvider.now();

        validateOpenTodayCanRun();

        var maxItems = Math.max(1, drawProps.getScheduler().getOpenToday().getMaxItemsPerRun());
        var activeTenants = tenantRegistry.listActiveTenantIds();
        if (activeTenants.isEmpty()) {
            throw new JobSkippedException("no_active_tenants", "No active tenants");
        }
        var failures = new ArrayList<TenantFailure>();

        for (TenantId tenantId : activeTenants) {
            try {
                var execution = batchJobStarter.start(DRAW_OPEN, openParamsFor(tenantId, maxItems));
                log.info(
                    "draw.open_today job started tenantId={} executionId={} now={} maxItems={}",
                    tenantId,
                    execution.jobExecutionId(),
                    now,
                    maxItems
                );

            } catch (Exception ex) {
                failures.add(new TenantFailure(tenantId, ex));

                log.warn(
                    "draw.open_today tenant failed tenantId={} err={}",
                    tenantId,
                    ex.getMessage(),
                    ex
                );
            }
        }

        if (!failures.isEmpty()) {
            throw new JobPartialFailureException(
                "draw_open_today_partial_failure",
                "Draw open today failed for " + failures.size() + " failures"
            );
        }
    }


    private void validateOpenTodayCanRun() {
        if (!drawProps.getScheduler().isActive()) {
            throw new JobSkippedException("scheduler_disabled", "Draw scheduler disabled");
        }
        if (!drawProps.getScheduler().getOpenToday().isActive()) {
            throw new JobSkippedException("open_today_disabled", "Draw open-today scheduler disabled");
        }
        if (!batchGate.enabled(DRAW_OPEN, null)) {
            throw new JobSkippedException("gate_disabled", "Draw open gate disabled");
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

    private HashMap<String, String> generateParamsFor(
        TenantId tenantId,
        java.time.LocalDate from,
        java.time.LocalDate to,
        int daysAhead
    ) {
        var params = baseParams(tenantId, "draw-generate");
        params.put(FROM, from.toString());
        params.put(TO, to.toString());
        params.put(DAYS_AHEAD, Integer.toString(daysAhead));
        params.put(JobParamKeys.DRY_RUN, Boolean.toString(DEFAULT_DRY_RUN));
        params.put(JobParamKeys.FORCE, Boolean.toString(DEFAULT_FORCE));
        return params;
    }

    private HashMap<String, String> openParamsFor(TenantId tenantId, int maxItems) {
        var params = baseParams(tenantId, "draw-open");
        params.put(MAX_ITEMS, Integer.toString(maxItems));
        params.put(JobParamKeys.DRY_RUN, Boolean.toString(DEFAULT_DRY_RUN));
        return params;
    }

    private static HashMap<String, String> baseParams(TenantId tenantId, String kind) {
        var params = new HashMap<String, String>();
        params.put(JobParamKeys.TENANT_ID, tenantId.value().toString());
        params.put(JobParamKeys.REQUEST_ID, requestId(kind));
        params.put(JobParamKeys.ACTOR, "scheduler");
        return params;
    }

    private static String requestId(String kind) {
        return kind + "-" + UUID.randomUUID();
    }

    private record TenantFailure(TenantId tenantId, Exception cause) {
    }
}
