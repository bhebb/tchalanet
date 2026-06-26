package com.tchalanet.server.core.draw.internal.infra.scheduler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawProcessingCandidateReaderPort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawProcessingCandidateReaderPort.DrawProcessingSlotDate;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawProcessingTickScheduler {

    private static final boolean DEFAULT_DRY_RUN = false;
    private static final boolean DEFAULT_FORCE = false;
    private static final JobKey DRAW_PROCESSING = JobKey.of("draw:processing:enabled");
    private static final JobKey DRAW_CLOSE = JobKey.of("draw:lifecycle:close");
    private static final JobKey DRAW_SETTLE = JobKey.of("draw:lifecycle:settle");
    private static final JobKey RESULTS_EXTERNAL_FETCH = JobKey.of("results:external:fetch");
    private static final JobKey RESULTS_EXTERNAL_APPLY = JobKey.of("results:external:apply");
    private static final String DATE = "date";
    private static final String DAYS_BACK = "days_back";
    private static final String MAX_DRAWS = "max_draws";
    private static final String MAX_ITEMS = "max_items";
    private static final String MAX_SLOTS = "max_slots";
    private static final String SLOT_KEYS = "slot_keys";

    private final BatchJobStarter batchJobStarter;
    private final BatchGate gate;
    private final TenantPreContextLookupApi tenantRegistry;
    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawResultReaderPort drawResultReader;
    private final JobContextBinder binder;
    private final DrawProperties drawProperties;
    private final DrawProcessingDuePolicy duePolicy;
    private final Clock clock;
    private final JsonUtils jsonUtils;
    private final AtomicBoolean configLogged = new AtomicBoolean(false);
    private final DrawProcessingCandidateReaderPort candidateReader;

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

        var tenants = tenantRegistry.listActiveTenantIds();
        int processed = 0;
        int errors = 0;

        for (TenantId tenantId : tenants) {
            try {
                var execution = batchJobStarter.start(
                    DRAW_CLOSE,
                    closeParamsFor(tenantId, Math.max(1, cfg.getMaxItemsPerTick()), now));
                log.info(
                    "draw.processing.close job started tenantId={} executionId={}",
                    tenantId,
                    execution.jobExecutionId());
                processed++;
            } catch (Exception ex) {
                errors++;
                log.warn("draw.processing.close tenant failed tenantId={} err={}", tenantId, ex.getMessage(), ex);
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
                var execution = batchJobStarter.start(RESULTS_EXTERNAL_FETCH, fetchParamsFor(candidate, now));
                duePolicy.markRun("fetch", candidate.slot().slotKey(), candidate.drawDate(), now);
                log.info(
                    "draw.processing.fetch job started slot={} drawDate={} executionId={}",
                    candidate.slot().slotKey(),
                    candidate.drawDate(),
                    execution.jobExecutionId());
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

        if (!cfg.isActive()) {
            return StepSummary.skipped("inactive");
        }

        if (!gate.enabled(RESULTS_EXTERNAL_APPLY, null)) {
            return StepSummary.skipped("gate_disabled");
        }

        var due =
            dueSlotDates("apply", now, cfg).stream()
                .filter(this::hasAnyResult)
                .limit(Math.max(1, cfg.getMaxItemsPerTick()))
                .toList();

        if (due.isEmpty()) {
            return StepSummary.skipped("no_due_results");
        }

        var dueByDate =
            due.stream()
                .collect(
                    Collectors.groupingBy(
                        SlotDate::drawDate,
                        LinkedHashMap::new,
                        Collectors.toList()));

        var tenants = tenantRegistry.listActiveTenantIds();

        int processed = 0;
        int skippedNoCandidates = 0;
        int errors = 0;

        for (var entry : dueByDate.entrySet()) {
            var drawDate = entry.getKey();
            var candidates = entry.getValue();

            var processingCandidates = toProcessingCandidates(candidates);

            if (processingCandidates.isEmpty()) {
                log.info(
                    "draw.processing.apply.skip no_processing_candidates drawDate={}",
                    drawDate);
                continue;
            }

            var slotKeys =
                processingCandidates.stream()
                    .map(DrawProcessingSlotDate::slotKey)
                    .filter(key -> key != null && !key.isBlank())
                    .map(key -> key.trim().toUpperCase(java.util.Locale.ROOT))
                    .distinct()
                    .limit(Math.max(1, cfg.getMaxItemsPerTick()))
                    .toList();

            if (slotKeys.isEmpty()) {
                continue;
            }

            for (TenantId tenantId : tenants) {
                try {
                    binder.bindTenant(tenantId, "scheduler");

                    if (!candidateReader.hasApplyCandidates(processingCandidates)) {
                        skippedNoCandidates++;
                        log.debug(
                            "draw.processing.apply.skip no_candidates tenantId={} drawDate={} slots={}",
                            tenantId,
                            drawDate,
                            slotKeys);
                        continue;
                    }

                    var execution = batchJobStarter.start(
                        RESULTS_EXTERNAL_APPLY,
                        applyParamsFor(tenantId, drawDate, slotKeys, now));
                    log.info(
                        "draw.processing.apply job started tenantId={} drawDate={} slots={} executionId={}",
                        tenantId,
                        drawDate,
                        slotKeys,
                        execution.jobExecutionId());

                    processed++;

                } catch (Exception ex) {
                    errors++;
                    log.warn(
                        "draw.processing.apply tenant failed tenantId={} drawDate={} slots={} err={}",
                        tenantId,
                        drawDate,
                        slotKeys,
                        ex.getMessage(),
                        ex);
                } finally {
                    clearContext("draw.processing.apply", tenantId);
                }
            }

            for (var candidate : candidates) {
                duePolicy.markRun("apply", candidate.slot().slotKey(), candidate.drawDate(), now);
            }
        }

        if (processed == 0 && errors == 0) {
            log.debug(
                "draw.processing.apply.skip no_tenant_candidates dueCandidates={} skippedNoCandidates={}",
                due.size(),
                skippedNoCandidates);
        }

        return new StepSummary(processed, due.size(), errors, null);
    }

    private List<DrawProcessingSlotDate> toProcessingCandidates(List<SlotDate> due) {
        if (due == null || due.isEmpty()) {
            return List.of();
        }

        var out = new ArrayList<DrawProcessingSlotDate>();

        for (var candidate : due) {
            var slot = candidate.slot();

            if (slot == null || slot.id() == null || slot.drawTime() == null || slot.timezone() == null) {
                continue;
            }

            try {
                var expectedOccurredAt =
                    OccurredAtResolver.resolveOrThrow(
                        null,
                        candidate.drawDate(),
                        slot.drawTime(),
                        slot.timezone());

                out.add(
                    new DrawProcessingSlotDate(
                        slot.id(),
                        slot.slotKey(),
                        candidate.drawDate(),
                        expectedOccurredAt));

            } catch (Exception ex) {
                log.debug(
                    "draw.processing.candidate.skip invalid_occurred_at slot={} drawDate={} err={}",
                    slot.slotKey(),
                    candidate.drawDate(),
                    ex.getMessage());
            }
        }

        return List.copyOf(out);
    }

    private StepSummary runSettle(Instant now) {
        var cfg = drawProperties.getScheduler().getProcessing().getSettle();

        if (!cfg.isActive()) {
            return StepSummary.skipped("inactive");
        }

        if (!gate.enabled(DRAW_SETTLE, null)) {
            return StepSummary.skipped("gate_disabled");
        }

        var due =
            dueSlotDates("settle", now, cfg).stream()
                .filter(this::hasAnyResult)
                .limit(Math.max(1, cfg.getMaxItemsPerTick()))
                .toList();

        if (due.isEmpty()) {
            return StepSummary.skipped("no_due_results");
        }

        var processingCandidates = toProcessingCandidates(due);

        if (processingCandidates.isEmpty()) {
            return StepSummary.skipped("no_processing_candidates");
        }

        var tenants = tenantRegistry.listActiveTenantIds();

        int processed = 0;
        int skippedNoCandidates = 0;
        int errors = 0;

        for (TenantId tenantId : tenants) {
            if (!gate.enabled(DRAW_SETTLE, tenantId)) {
                continue;
            }

            try {
                binder.bindTenant(tenantId, "scheduler");

                if (!candidateReader.hasSettleCandidates(processingCandidates)) {
                    skippedNoCandidates++;
                    log.debug(
                        "draw.processing.settle.skip no_candidates tenantId={} dueCandidates={}",
                        tenantId,
                        processingCandidates.size());
                    continue;
                }

                var exec = batchJobStarter.start(DRAW_SETTLE, settleParamsFor(tenantId, now));

                log.info(
                    "draw.processing.settle started tenantId={} executionId={} dueCandidates={}",
                    tenantId,
                    exec.jobExecutionId(),
                    processingCandidates.size());

                processed++;

            } catch (Exception ex) {
                errors++;
                log.warn(
                    "draw.processing.settle tenant failed tenantId={} err={}",
                    tenantId,
                    ex.getMessage(),
                    ex);
            } finally {
                clearContext("draw.processing.settle", tenantId);
            }
        }

        if (processed > 0 || errors > 0) {
            due.forEach(
                candidate ->
                    duePolicy.markRun("settle", candidate.slot().slotKey(), candidate.drawDate(), now));
        } else {
            log.debug(
                "draw.processing.settle.skip no_tenant_candidates dueCandidates={} skippedNoCandidates={}",
                due.size(),
                skippedNoCandidates);
        }

        return new StepSummary(processed, due.size(), errors, null);
    }

    private List<SlotDate> dueSlotDates(String step, Instant now, DrawProperties.DueAfterDraw config) {
        var candidates = new ArrayList<SlotDate>();
        List<?> activeSlots = resultSlotCatalog.listActive();
        for (Object rawSlot : activeSlots) {
            var slot = toResultSlotView(rawSlot);
            if (slot == null) {
                continue;
            }
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

    private ResultSlotView toResultSlotView(Object rawSlot) {
        if (rawSlot instanceof ResultSlotView slotView) {
            return slotView;
        }

        try {
            return jsonUtils.convertValue(rawSlot, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("draw.processing.slot.invalid type={} err={}",
                rawSlot == null ? "null" : rawSlot.getClass().getName(),
                ex.getMessage());
            return null;
        }
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
        params.put(DAYS_BACK, "1");
        params.put(
            MAX_DRAWS,
            Integer.toString(Math.max(1, drawProperties.getScheduler().getProcessing().getSettle().getMaxItemsPerTick())));
        params.put(JobParamKeys.DRY_RUN, Boolean.toString(DEFAULT_DRY_RUN));
        params.put(JobParamKeys.FORCE, Boolean.toString(DEFAULT_FORCE));
        return params;
    }

    private HashMap<String, String> closeParamsFor(TenantId tenantId, int maxItems, Instant now) {
        var params = jobParams(tenantId, "draw-close", now);
        params.put(MAX_ITEMS, Integer.toString(maxItems));
        params.put(JobParamKeys.DRY_RUN, Boolean.toString(DEFAULT_DRY_RUN));
        return params;
    }

    private HashMap<String, String> fetchParamsFor(SlotDate candidate, Instant now) {
        var params = new HashMap<String, String>();
        params.put(JobParamKeys.REQUEST_ID, requestId("results-fetch", now));
        params.put(JobParamKeys.ACTOR, "scheduler");
        params.put(DATE, candidate.drawDate().toString());
        params.put(DAYS_BACK, "0");
        params.put(SLOT_KEYS, candidate.slot().slotKey());
        params.put(MAX_SLOTS, "1");
        params.put(JobParamKeys.DRY_RUN, Boolean.toString(DEFAULT_DRY_RUN));
        params.put(JobParamKeys.FORCE, Boolean.toString(DEFAULT_FORCE));
        return params;
    }

    private HashMap<String, String> applyParamsFor(
        TenantId tenantId,
        LocalDate drawDate,
        List<String> slotKeys,
        Instant now
    ) {
        var params = jobParams(tenantId, "results-apply", now);
        params.put(DATE, drawDate.toString());
        params.put(DAYS_BACK, "0");
        params.put(SLOT_KEYS, String.join(",", slotKeys));
        params.put(MAX_SLOTS, Integer.toString(Math.max(1, slotKeys.size())));
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

    private record SlotDate(ResultSlotView slot, LocalDate drawDate) {
    }

    private record StepSummary(int processed, int candidates, int errors, String skippedReason) {
        static StepSummary skipped(String reason) {
            return new StepSummary(0, 0, 0, reason);
        }
    }
}
