package com.tchalanet.server.core.draw.internal.infra.scheduler;

import static com.tchalanet.server.common.job.key.BatchJobKeys.DRAW_CLOSE;
import static com.tchalanet.server.common.job.key.BatchJobKeys.DRAW_PROCESSING;
import static com.tchalanet.server.common.job.key.BatchJobKeys.DRAW_SETTLE;
import static com.tchalanet.server.common.job.key.BatchJobKeys.RESULTS_EXTERNAL_APPLY;
import static com.tchalanet.server.common.job.key.BatchJobKeys.RESULTS_EXTERNAL_FETCH;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.job.context.JobContextBindingRequest;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.api.command.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import com.tchalanet.server.core.drawresult.api.command.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawProcessingTickScheduler {

    private static final boolean DEFAULT_DRY_RUN = false;
    private static final boolean DEFAULT_FORCE = false;

    private final CommandBus commandBus;
    private final BatchJobStarter batchJobStarter;
    private final BatchGate gate;
    private final TenantCatalog tenantCatalog;
    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawResultReaderPort drawResultReader;
    private final JobContextBinder binder;
    private final DrawProperties drawProperties;
    private final DrawProcessingDuePolicy duePolicy;
    private final Clock clock;
    private final AtomicBoolean configLogged = new AtomicBoolean(false);

    @TchJob("draw:processing")
    @Scheduled(cron = "${tch.draw.scheduler.processing.cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_processing_tick", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void tick() {
        log.info("draw.processing.tick fired");
        logEffectiveConfigOnce();

        if (!canRunProcessing()) {
            return;
        }

        var now = clock.instant();
        var closeSummary = runClose(now);
        var fetchSummary = runFetch(now);
        var applySummary = runApply(now);
        var settleSummary = runSettle(now);

        log.info(
            "draw.processing.tick summary close={} fetch={} apply={} settle={}",
            closeSummary,
            fetchSummary,
            applySummary,
            settleSummary);
    }

    private boolean canRunProcessing() {
        var scheduler = drawProperties.getScheduler();
        if (!scheduler.isActive()) {
            log.info("draw.processing.tick skipped reason=scheduler_disabled");
            return false;
        }
        if (!scheduler.getProcessing().isActive()) {
            log.info("draw.processing.tick skipped reason=processing_disabled");
            return false;
        }
        if (!gate.enabled(DRAW_PROCESSING, null)) {
            log.info("draw.processing.tick skipped reason=processing_gate_disabled");
            return false;
        }
        return true;
    }

    private StepSummary runClose(Instant now) {
        var cfg = drawProperties.getScheduler().getProcessing().getClose();
        if (!cfg.isActive()) return StepSummary.skipped("inactive");
        if (!gate.enabled(DRAW_CLOSE, null)) return StepSummary.skipped("gate_disabled");

        var tenants = tenantCatalog.listActiveTenantIds();
        int processed = 0;
        int errors = 0;

        for (TenantId tenantId : tenants) {
            try {
                binder.bind(JobContextBindingRequest.tenant(jobParams(tenantId, "draw-close", now)));
                commandBus.execute(new CloseDueDrawsCommand(
                    now,
                    Math.max(1, cfg.getMaxItemsPerTick()),
                    DEFAULT_DRY_RUN));
                processed++;
            } catch (Exception ex) {
                errors++;
                log.warn("draw.processing.close tenant failed tenantId={} err={}", tenantId, ex.getMessage(), ex);
            } finally {
                clearContext("draw.processing.close", tenantId);
            }
        }
        return new StepSummary(processed, 0, errors, null);
    }

    private StepSummary runFetch(Instant now) {
        var cfg = drawProperties.getScheduler().getProcessing().getFetch();
        if (!cfg.isActive()) return StepSummary.skipped("inactive");
        if (!gate.enabled(RESULTS_EXTERNAL_FETCH, null)) return StepSummary.skipped("gate_disabled");

        var due = dueSlotDates("fetch", now, cfg).stream()
            .filter(candidate -> !hasConfirmedResult(candidate))
            .limit(Math.max(1, cfg.getMaxSlotsPerTick()))
            .toList();

        int processed = 0;
        int errors = 0;

        for (SlotDate candidate : due) {
            try {
                commandBus.execute(new FetchExternalResultsWindowCommand(
                    null,
                    candidate.drawDate(),
                    0,
                    List.of(candidate.slot().slotKey()),
                    DEFAULT_FORCE,
                    DEFAULT_DRY_RUN,
                    1,
                    null,
                    false));
                duePolicy.markRun("fetch", candidate.slot().slotKey(), candidate.drawDate(), now);
                processed++;
            } catch (Exception ex) {
                errors++;
                log.warn(
                    "draw.processing.fetch slot failed slot={} drawDate={} err={}",
                    candidate.slot().slotKey(),
                    candidate.drawDate(),
                    ex.getMessage(),
                    ex);
            }
        }
        return new StepSummary(processed, due.size(), errors, null);
    }

    private StepSummary runApply(Instant now) {
        var cfg = drawProperties.getScheduler().getProcessing().getApply();
        if (!cfg.isActive()) return StepSummary.skipped("inactive");
        if (!gate.enabled(RESULTS_EXTERNAL_APPLY, null)) return StepSummary.skipped("gate_disabled");

        var due = dueSlotDates("apply", now, cfg).stream()
            .filter(this::hasAnyResult)
            .limit(Math.max(1, cfg.getMaxItemsPerTick()))
            .toList();

        var tenants = tenantCatalog.listActiveTenantIds();
        int processed = 0;
        int errors = 0;

        for (SlotDate candidate : due) {
            for (TenantId tenantId : tenants) {
                try {
                    binder.bind(JobContextBindingRequest.tenant(jobParams(tenantId, "draw-results-apply", now)));
                    commandBus.execute(new ApplyExternalResultsWindowCommand(
                        tenantId,
                        candidate.drawDate(),
                        0,
                        List.of(candidate.slot().slotKey()),
                        DEFAULT_FORCE,
                        DEFAULT_DRY_RUN,
                        1,
                        null));
                    processed++;
                } catch (Exception ex) {
                    errors++;
                    log.warn(
                        "draw.processing.apply tenant failed tenantId={} slot={} drawDate={} err={}",
                        tenantId,
                        candidate.slot().slotKey(),
                        candidate.drawDate(),
                        ex.getMessage(),
                        ex);
                } finally {
                    clearContext("draw.processing.apply", tenantId);
                }
            }
            duePolicy.markRun("apply", candidate.slot().slotKey(), candidate.drawDate(), now);
        }
        return new StepSummary(processed, due.size(), errors, null);
    }

    private StepSummary runSettle(Instant now) {
        var cfg = drawProperties.getScheduler().getProcessing().getSettle();
        if (!cfg.isActive()) return StepSummary.skipped("inactive");
        if (!gate.enabled(DRAW_SETTLE, null)) return StepSummary.skipped("gate_disabled");

        var due = dueSlotDates("settle", now, cfg).stream()
            .filter(this::hasAnyResult)
            .limit(Math.max(1, cfg.getMaxItemsPerTick()))
            .toList();
        if (due.isEmpty()) return new StepSummary(0, 0, 0, null);

        var tenants = tenantCatalog.listActiveTenantIds();
        int processed = 0;
        int errors = 0;

        for (TenantId tenantId : tenants) {
            if (!gate.enabled(DRAW_SETTLE, tenantId)) {
                continue;
            }
            try {
                var exec = batchJobStarter.start(DRAW_SETTLE, settleParamsFor(tenantId, now));
                log.info(
                    "draw.processing.settle started tenantId={} executionId={} dueCandidates={}",
                    tenantId,
                    exec.jobExecutionId(),
                    due.size());
                processed++;
            } catch (Exception ex) {
                errors++;
                log.warn("draw.processing.settle tenant failed tenantId={} err={}", tenantId, ex.getMessage(), ex);
            }
        }

        due.forEach(candidate -> duePolicy.markRun("settle", candidate.slot().slotKey(), candidate.drawDate(), now));
        return new StepSummary(processed, due.size(), errors, null);
    }

    private List<SlotDate> dueSlotDates(String step, Instant now, DrawProperties.DueAfterDraw config) {
        var candidates = new ArrayList<SlotDate>();
        for (ResultSlotView slot : resultSlotCatalog.listActive()) {
            if (!slot.active() || slot.drawTime() == null || slot.timezone() == null) {
                continue;
            }
            var today = LocalDate.now(clock.withZone(slot.timezone()));
            for (LocalDate drawDate : List.of(today, today.minusDays(1))) {
                if (duePolicy.isDue(step, slot, drawDate, now, config)) {
                    candidates.add(new SlotDate(slot, drawDate));
                }
            }
        }
        return candidates;
    }

    private boolean hasConfirmedResult(SlotDate candidate) {
        var occurredAt = occurredAt(candidate);
        return drawResultReader.findViewBySlotKeyAndOccurredAt(candidate.slot().slotKey(), occurredAt)
            .map(view -> DrawResultStatus.CONFIRMED == view.status())
            .orElse(false);
    }

    private boolean hasAnyResult(SlotDate candidate) {
        return drawResultReader
            .findViewBySlotKeyAndOccurredAt(candidate.slot().slotKey(), occurredAt(candidate))
            .isPresent();
    }

    private static Instant occurredAt(SlotDate candidate) {
        return OccurredAtResolver.resolveOrThrow(
            null,
            candidate.drawDate(),
            candidate.slot().drawTime(),
            candidate.slot().timezone());
    }

    private HashMap<String, String> settleParamsFor(TenantId tenantId, Instant now) {
        var params = new HashMap<String, String>();
        params.put(JobParamKeys.TENANT_ID, tenantId.value().toString());
        params.put(JobParamKeys.REQUEST_ID, requestId("draw-settle", now));
        params.put(JobParamKeys.ACTOR, "scheduler");
        params.put(JobParamKeys.DAYS_BACK, "1");
        params.put(
            JobParamKeys.MAX_DRAWS,
            Integer.toString(Math.max(1, drawProperties.getScheduler().getProcessing().getSettle().getMaxItemsPerTick())));
        params.put(JobParamKeys.DRY_RUN, Boolean.toString(DEFAULT_DRY_RUN));
        params.put(JobParamKeys.FORCE, Boolean.toString(DEFAULT_FORCE));
        return params;
    }

    private HashMap<String, String> jobParams(
        TenantId tenantId,
        String kind,
        Instant now
    ) {
        var params = new HashMap<String, String>();
        params.put(JobParamKeys.TENANT_ID, tenantId.value().toString());
        params.put(JobParamKeys.REQUEST_ID, requestId(kind, now));
        params.put(JobParamKeys.ACTOR, "scheduler");
        return params;
    }

    private static String requestId(String kind, Instant now) {
        return kind + "-" + now.toEpochMilli() + "-" + UUID.randomUUID();
    }

    private void clearContext(String source, TenantId tenantId) {
        try {
            binder.clear();
        } catch (Exception ex) {
            log.error("{} failed to clear context tenantId={} err={}", source, tenantId, ex.getMessage(), ex);
        }
    }

    private void logEffectiveConfigOnce() {
        if (!configLogged.compareAndSet(false, true)) {
            return;
        }
        var processing = drawProperties.getScheduler().getProcessing();
        log.info(
            "draw.processing.config active={} cron={} timezone={} close.max={} fetch.start={} fetch.retry={} fetch.stop={} fetch.max={} apply.start={} apply.retry={} apply.stop={} apply.max={} settle.start={} settle.retry={} settle.stop={} settle.max={}",
            processing.isActive(),
            processing.getCron(),
            processing.getTimezone(),
            processing.getClose().getMaxItemsPerTick(),
            processing.getFetch().getStartMinutesAfterDraw(),
            processing.getFetch().getRetryEveryMinutes(),
            processing.getFetch().getStopMinutesAfterDraw(),
            processing.getFetch().getMaxSlotsPerTick(),
            processing.getApply().getStartMinutesAfterDraw(),
            processing.getApply().getRetryEveryMinutes(),
            processing.getApply().getStopMinutesAfterDraw(),
            processing.getApply().getMaxItemsPerTick(),
            processing.getSettle().getStartMinutesAfterDraw(),
            processing.getSettle().getRetryEveryMinutes(),
            processing.getSettle().getStopMinutesAfterDraw(),
            processing.getSettle().getMaxItemsPerTick());
    }

    private record SlotDate(ResultSlotView slot, LocalDate drawDate) {}

    private record StepSummary(int processed, int candidates, int errors, String skippedReason) {
        static StepSummary skipped(String reason) {
            return new StepSummary(0, 0, 0, reason);
        }
    }
}
