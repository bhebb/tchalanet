package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.DateWindows;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowResult;
import com.tchalanet.server.core.draw.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class ApplyExternalResultsWindowCommandHandler
    implements CommandHandler<ApplyExternalResultsWindowCommand, ApplyExternalResultsWindowResult> {


    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawResultReaderPort drawResultReader;
    private final DrawApplyPort drawApply;
    private final DrawResultsProperties props;
    private final Clock clock;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public ApplyExternalResultsWindowResult handle(ApplyExternalResultsWindowCommand cmd) {
        validate(cmd);

        int applied = 0;
        int alreadyOrNotEligible = 0;
        int skippedDryRun = 0;
        int skippedPending = 0;
        int slotNotFound = 0;
        int slotInactive = 0;
        int errors = 0;

        int daysBack = clampDaysBack(cmd.daysBack());
        int maxSlots = Math.min(cmd.maxSlots(), props.getLimits().getMaxSlotsPerTick());

        var now = clock.instant();
        var eventTime = now;

        var slotKeys =
            cmd.slotKeys().stream()
                .map(ApplyExternalResultsWindowCommandHandler::normalizeKey)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(maxSlots)
                .toList();

        var slotsByKey = new java.util.HashMap<String, ResultSlotView>();

        for (String slotKey : slotKeys) {
            var slotOpt = resultSlotCatalog.findByKey(slotKey);

            if (slotOpt.isEmpty()) {
                slotNotFound++;
                continue;
            }

            var slot = slotOpt.get();

            if (!slot.active()) {
                slotInactive++;
                continue;
            }

            slotsByKey.put(normalizeKey(slot.slotKey()), slot);
        }

        var eventsToPublish = new java.util.ArrayList<DrawResultAppliedEvent>();

        for (LocalDate date : DateWindows.datesBackInclusive(cmd.baseDate(), daysBack)) {
            for (String slotKey : slotKeys) {
                var slot = slotsByKey.get(slotKey);

                if (slot == null) {
                    continue;
                }

                try {
                    Instant occurredAt =
                        OccurredAtResolver.resolveOrThrow(
                            null,
                            date,
                            slot.drawTime(),
                            slot.timezone());

                    var drawResultOpt =
                        drawResultReader.findByResultSlotIdAndOccurredAt(slot.id(), occurredAt);

                    if (drawResultOpt.isEmpty()) {
                        skippedPending++;
                        continue;
                    }

                    var drawResultId = drawResultOpt.get();

                    if (cmd.dryRun()) {
                        skippedDryRun++;
                        continue;
                    }

                    /*
                     * Replay/backfill rule:
                     * - can run for past dates;
                     * - can run repeatedly;
                     * - must stay idempotent;
                     * - must not replace an already applied draw_result_id.
                     *
                     * Correction/replacement belongs to CorrectAppliedDrawResultCommand.
                     */
                    var res =
                        drawApply.attachResultBySlot(
                            cmd.tenantId(),
                            date,
                            slot.id(),
                            drawResultId,
                            now,
                            false);

                    if (res.outcome() == DrawApplyPort.ApplyOutcome.UPDATED
                        && !res.applied().isEmpty()) {

                        applied += res.applied().size();

                        for (var d : res.applied()) {
                            eventsToPublish.add(
                                new DrawResultAppliedEvent(
                                    EventId.of(idGenerator.newUuid()),
                                    eventTime,
                                    cmd.tenantId(),
                                    d.drawId(),
                                    date,
                                    slot.id(),
                                    drawResultId,
                                    d.drawChannelId()));
                        }
                    } else {
                        alreadyOrNotEligible++;
                    }

                } catch (Exception e) {
                    errors++;

                    log.warn(
                        "draw.results.apply failed tenant={} slot={} date={} dryRun={} force={} err={}",
                        cmd.tenantId(),
                        slotKey,
                        date,
                        cmd.dryRun(),
                        cmd.force(),
                        e.getMessage(),
                        e);
                }
            }
        }

        if (!eventsToPublish.isEmpty()) {
            AfterCommit.run(() -> eventsToPublish.forEach(publisher::publish));
        }

        log.info(
            "draw.results.apply tenant={} baseDate={} daysBack={} dryRun={} force={} applied={} slotInactive={} alreadyOrNotEligible={} skippedDryRun={} skippedPending={} slotNotFound={} errors={}",
            cmd.tenantId(),
            cmd.baseDate(),
            daysBack,
            cmd.dryRun(),
            cmd.force(),
            applied,
            slotInactive,
            alreadyOrNotEligible,
            skippedDryRun,
            skippedPending,
            slotNotFound,
            errors);

        return new ApplyExternalResultsWindowResult(
            applied,
            slotInactive,
            alreadyOrNotEligible,
            skippedDryRun + skippedPending,
            slotNotFound,
            errors);
    }


    private int clampDaysBack(int v) {
        int x = Math.max(0, v);
        return Math.min(x, props.getLimits().getHardDaysBack());
    }

    private static void validate(ApplyExternalResultsWindowCommand cmd) {
        Objects.requireNonNull(cmd, "command is required");
        Objects.requireNonNull(cmd.tenantId(), "tenantId required");
        Objects.requireNonNull(cmd.baseDate(), "baseDate required");
        if (cmd.daysBack() < 0) throw new IllegalArgumentException("daysBack must be >= 0");
        if (cmd.slotKeys() == null || cmd.slotKeys().isEmpty()) throw new IllegalArgumentException("slotKeys required");
        if (cmd.maxSlots() <= 0) throw new IllegalArgumentException("maxSlots must be > 0");
    }

    private static String normalizeKey(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
