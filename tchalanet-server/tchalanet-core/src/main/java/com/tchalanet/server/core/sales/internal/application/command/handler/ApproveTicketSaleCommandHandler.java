package com.tchalanet.server.core.sales.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.core.sales.api.command.ApproveTicketSaleCommand;
import com.tchalanet.server.core.sales.api.command.TicketApprovedResult;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.internal.domain.event.TicketPlacedLineEvent;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import com.tchalanet.server.core.sales.internal.application.service.TicketSalePolicyService;
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
            throw ProblemRest.conflict(
                "Ticket is not pending approval. saleStatus=" + ticket.getSaleStatus());
        }

        Instant now = Instant.now(clock);

        try {
            salePolicy.resolveAndValidateDraw(ticket.getDrawId());
        } catch (RuntimeException ex) {
            throw ProblemRest.conflict("Draw cutoff exceeded, cannot approve this ticket");
        }

        if (ticket.getSalesSessionId() != null) {
            salePolicy.validateSession(ticket.getTenantId(), ticket.getTerminalId());
        }

        ticket.approve(now);
        var saved = ticketWriter.save(ticket);

        List<TicketPlacedLineEvent> lines = saved.getLines().stream()
            .map(l -> new TicketPlacedLineEvent(
                l.betType(),
                l.selection(),
                l.stakeAmount().movePointRight(2).longValue(),
                l.potentialPayoutAmount().movePointRight(2).longValue(),
                l.betOption()
            ))
            .toList();

        var placed =
            new TicketPlacedEvent(
                EventId.of(UUID.randomUUID()),
                now,
                saved.getTenantId(),
                saved.getId(),
                saved.getOutletId(),
                saved.getSellerUserId(),
                saved.getTerminalId(),
                saved.getSalesSessionId(),
                saved.getDrawId(),
                saved.getDrawChannelId(),
                saved.getMoney().stakeAmount().movePointRight(2).longValue(),
                saved.getMoney().feeAmount().movePointRight(2).longValue(),
                saved.getMoney().totalAmount().movePointRight(2).longValue(),
                saved.getCurrency(),
                saved.getSaleOrigin(),
                saved.getSyncStatus(),
                lines);

        AfterCommit.run(() -> publisher.publish(placed));

        log.info("Ticket approved ticketId={} saleStatus={}", saved.getId(), saved.getSaleStatus());
        return new TicketApprovedResult(saved);
    }
}
