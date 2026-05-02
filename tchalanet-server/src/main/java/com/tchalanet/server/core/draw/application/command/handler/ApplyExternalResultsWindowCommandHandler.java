package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.config.draw.DrawResultsCommonProperties;
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
    private final DrawResultsCommonProperties props;
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
        int skippedPending = 0; // draw_result absent (fetch pas prêt) => retry next tick
        int notFound = 0;       // slot missing/inactive only
        int errors = 0;

        int daysBack = clampDaysBack(cmd.daysBack());
        Instant now = Instant.now(clock);

        var slotKeys =
            cmd.slotKeys().stream()
                .map(ApplyExternalResultsWindowCommandHandler::normalizeKey)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(cmd.maxSlots())
                .toList();

        for (LocalDate date : DateWindows.datesBackInclusive(cmd.baseDate(), daysBack)) {
            for (String slotKey : slotKeys) {
                try {
                    var slotOpt = resultSlotCatalog.findByKey(slotKey);
                    if (slotOpt.isEmpty()) {
                        notFound++;
                        continue;
                    }
                    var slot = slotOpt.get();
                    if (!slot.active()) {
                        notFound++;
                        continue;
                    }

                    // Deterministic occurredAt for slot/date (timezone + drawTime)
                    Instant occurredAt =
                        OccurredAtResolver.resolve(null, date, slot.drawTime(), slot.timezone(), clock);

                    var drOpt = drawResultReader.findByResultSlotIdAndOccurredAt(slot.id(), occurredAt);
                    if (drOpt.isEmpty()) {
                        skippedPending++;
                        continue;
                    }
                    var drawResultId = drOpt.get();

                    if (cmd.dryRun()) {
                        skippedDryRun++;
                        continue;
                    }

                    var res =
                        drawApply.attachResultBySlot(
                            cmd.tenantId(), date, slot.id(), drawResultId, now, cmd.force());

                    if (res.outcome() == DrawApplyPort.ApplyOutcome.UPDATED && !res.applied().isEmpty()) {
                        applied += res.applied().size();

                        AfterCommit.run(
                            () -> {
                                for (var d : res.applied()) {

                                    var appliedEvent =
                                        new DrawResultAppliedEvent(
                                            EventId.of(idGenerator.newUuid()),
                                            Instant.now(clock),
                                            cmd.tenantId(),
                                            d.drawId(),
                                            slot.id(),
                                            drawResultId);
                                    publisher.publish(appliedEvent);
                                }
                            });

                    } else {
                        alreadyOrNotEligible++;
                    }

                } catch (Exception e) {
                    errors++;
                    log.warn(
                        "draw-results.apply failed tenant={} slot={} date={} err={}",
                        cmd.tenantId(),
                        slotKey,
                        date,
                        e.getLocalizedMessage(), e);
                }
            }
        }

        return new ApplyExternalResultsWindowResult(
            applied,
            /* updated */ 0,
            /* already */ alreadyOrNotEligible,
            /* skipped */ skippedDryRun + skippedPending,
            /* notFound */ notFound,
            /* errors */ errors);
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
