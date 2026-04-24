package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.payout.application.command.model.MarkTicketPayoutPendingCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketPaymentPendingEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import com.tchalanet.server.common.types.id.EventId;
import lombok.RequiredArgsConstructor;
@UseCase
@RequiredArgsConstructor
public class MarkTicketPayoutPendingCommandHandler implements CommandHandler<MarkTicketPayoutPendingCommand, Ticket> {

    private final TicketReaderPort ticketReader;
    private final TicketWritterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    @RequiresPermission("ticket.payout.mark_pending")
    public Ticket handle(MarkTicketPayoutPendingCommand command) {
        Ticket ticket = ticketReader.findWithLinesById(command.ticketId())
            .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

        Instant now = Instant.now(clock);

        // SETTLED_WON -> PAYOUT_PENDING
        ticket.markPayoutPending(now);

        Ticket saved = ticketWriter.save(ticket);

        AfterCommit.run(() -> publisher.publish(
            new TicketPaymentPendingEvent(
                EventId.of(UUID.randomUUID()),
                now,
                saved.tenantId(),
                saved.id(),
                command.reason(),
                command.performedBy()
            )
        ));

        return saved;
    }
}
