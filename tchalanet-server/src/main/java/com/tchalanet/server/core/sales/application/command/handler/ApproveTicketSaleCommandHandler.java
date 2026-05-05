package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.core.sales.application.command.model.ApproveTicketSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.TicketApprovedResult;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.application.service.TicketSalePolicyService;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ApproveTicketSaleCommandHandler
    implements CommandHandler<ApproveTicketSaleCommand, TicketApprovedResult> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final TicketSalePolicyService salePolicy;
    private final SalesSessionReaderPort posSessionReaderPort;

    private final com.tchalanet.server.common.event.DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    public TicketApprovedResult handle(ApproveTicketSaleCommand cmd) {

        Ticket ticket =
            ticketReader
                .findWithLinesById(cmd.ticketId())
                .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

        if (ticket.getSaleStatus() != TicketSaleStatus.PENDING_APPROVAL) {
            // 409 Conflict is more accurate than 500
            throw ProblemRest.conflict(
                "Ticket is not pending approval. saleStatus=" + ticket.getSaleStatus());
        }

        Instant now = Instant.now(clock);

        // 1) Validate draw cutoff (shared rule)
        try {
            salePolicy.resolveAndValidateDraw(ticket.getDrawId());
        } catch (RuntimeException ex) {
            throw ProblemRest.conflict("Draw cutoff exceeded, cannot approve this ticket");
        }

        // 2) Re-validate session/outlet if session exists (recommended)
        if (ticket.getSessionId() != null) {
            // Ensure session still exists / outlet not blocked
            var session =
                posSessionReaderPort
                    .findById(ticket.getSessionId())
                    .orElseThrow(() -> ProblemRest.conflict("Session not found for approval"));

            // optional but consistent with SELL
            salePolicy.validateSession(ticket.getTenantId(), ticket.getTerminalId());
        }

        // 3) Approve + persist
        ticket.approve(now);
        var saved = ticketWriter.save(ticket);

        // 4) Publish TicketPlacedEvent AFTER COMMIT
        var session =
            saved.getSessionId() == null
                ? null
                : posSessionReaderPort.findById(saved.getSessionId()).orElse(null);

        var currency = saved.getCurrency() == null ? "HTG" : saved.getCurrency();

        // build lines
        List<TicketPlacedEvent.Line> lines = saved.getLines().stream()
            .map(l -> new TicketPlacedEvent.Line(
                l.betType(),
                l.selection(),
                l.stake().movePointRight(2).longValue(),
                l.potentialPayout() == null ? 0L : l.potentialPayout().movePointRight(2).longValue(),
                l.betOption()
            ))
            .toList();

        var placed =
            new TicketPlacedEvent(
                EventId.of(UUID.randomUUID()),
                now,
                saved.getTenantId(),
                saved.getId(),
                session != null ? session.outletId() : null,
                session != null ? com.tchalanet.server.common.types.id.AgentId.of(session.userId().value()) : null,
                saved.getTerminalId(),
                saved.getSessionId(),
                saved.getDrawId(),
                null, // drawChannelId not available in approve flow
                saved.getLines().isEmpty() ? "" : saved.getLines().get(0).gameCode().name(),
                saved.getTotalAmount().movePointRight(2).longValue(),
                currency,
                lines);

        AfterCommit.run(() -> publisher.publish(placed));

        log.info("Ticket approved ticketId={} saleStatus={}", saved.getId(), saved.getSaleStatus());
        return new TicketApprovedResult(saved);
    }
}
