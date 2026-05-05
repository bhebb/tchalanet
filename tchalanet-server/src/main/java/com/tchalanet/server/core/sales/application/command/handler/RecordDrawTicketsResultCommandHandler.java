package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultProjection;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.sales.application.command.model.RecordDrawTicketsResultCommand;
import com.tchalanet.server.core.sales.application.command.model.RecordDrawTicketsResultResult;
import com.tchalanet.server.core.sales.application.port.out.TicketSettlementPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.service.DrawResultMatchView;
import com.tchalanet.server.core.sales.domain.service.TicketWinningCalculator;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordDrawTicketsResultCommandHandler
    implements CommandHandler<RecordDrawTicketsResultCommand, RecordDrawTicketsResultResult> {

    private static final int DEFAULT_BATCH_SIZE = 250;

    private final TicketSettlementPort ticketSettlementPort;
    private final DrawResultReaderPort drawResultProjectionCatalog;
    private final TicketWriterPort ticketWriter;
    private final TicketWinningCalculator winningCalculator;

    private final DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    public RecordDrawTicketsResultResult handle(RecordDrawTicketsResultCommand cmd) {
        // 1) Load draw result view once (global)
        DrawResultProjection resultView =
            drawResultProjectionCatalog
                .findProjectionById(cmd.drawResultId())
                .orElseThrow(() -> new IllegalStateException("DrawResult not found: " + cmd.drawResultId()));

        // adapter: port view -> domain DrawResultMatchView
        DrawResultMatchView domainView = toMatchView(resultView);

        log.info(
            "Start ticket settlement for tenantId={} drawId={} drawResultId={} occurredAt={}",
            cmd.tenantId(),
            cmd.drawId(),
            cmd.drawResultId(),
            cmd.occurredAt());

        // 2) Cursor for keyset pagination
        Instant cursorCreatedAt = Instant.EPOCH;
        UUID cursorId = new UUID(0L, 0L);

        long processed = 0;
        long won = 0;
        long lost = 0;

        while (true) {
            var batch =
                ticketSettlementPort.findNextBatchForDraw(
                    cmd.drawId(),
                    cursorCreatedAt,
                    cursorId,
                    DEFAULT_BATCH_SIZE);

            if (batch == null || batch.isEmpty()) {
                break;
            }

            for (Ticket ticket : batch) {
                // a) compute winning amount
                BigDecimal winningAmount = winningCalculator.calculateWinningAmount(ticket, domainView);

                // b) apply domain transition
                Instant now = Instant.now(clock);
                if (winningAmount == null) winningAmount = BigDecimal.ZERO;
                ticket.markResulted(winningAmount, now);

                // c) persist
                Ticket saved = ticketWriter.save(ticket);

                // d) publish event after commit (use split statuses)
                java.util.UUID rawEventId = java.util.UUID.randomUUID();
                var event =
                    new com.tchalanet.server.core.sales.domain.event.TicketResultedEvent(
                        com.tchalanet.server.common.types.id.EventId.of(rawEventId),
                        now,
                        saved.getTenantId(),
                        saved.getId(),
                        saved.getResultStatus(),
                        saved.getSettlementStatus(),
                        saved.getWinningAmount());

                AfterCommit.run(() -> publisher.publish(event));

                processed++;
                if (winningAmount.signum() > 0) won++;
                else lost++;
            }

            // 3) update cursor from last item (keyset)
            Ticket last = batch.getLast();
            cursorCreatedAt = last.getCreatedAt();
            cursorId = last.getId().value();

            if (batch.size() < DEFAULT_BATCH_SIZE) break;
        }

        log.info(
            "End ticket settlement for tenantId={} drawId={} processed={} won={} lost={}",
            cmd.tenantId(),
            cmd.drawId(),
            processed,
            won,
            lost);

        return new RecordDrawTicketsResultResult(processed, won, lost);
    }

    // --- adapter: convert port DrawResultMinimalView to DrawResultMatchView used by the calculator ---
    private static DrawResultMatchView toMatchView(DrawResultProjection v) {
        if (v == null) return new DrawResultMatchView() {
            @Override public String lot1() { return null; }
            @Override public String lot2() { return null; }
            @Override public String lot3() { return null; }
            @Override public String pick3() { return null; }
        };

        return new DrawResultMatchView() {
            @Override public String lot1() { return v.lot1(); }
            @Override public String lot2() { return v.lot2(); }
            @Override public String lot3() { return v.lot3(); }
            @Override public String pick3() { return v.lot4(); }
        };
    }
}
