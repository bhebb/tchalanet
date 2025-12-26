package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.sales.application.command.model.MarkPaymentPendingCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketPaymentPendingEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class MarkPaymentPendingCommandHandler
    implements CommandHandler<MarkPaymentPendingCommand, Ticket> {

    private final TicketReaderPort ticketReader;
    private final TicketWritterPort ticketWriter;
    private final DomainEventPublisher publisher; // optional but recommended
    private final Clock clock;

    @Override
    @TchTx
    @RequiresPermission("ticket.mark_payment_pending")
    public Ticket handle(MarkPaymentPendingCommand command) {
        var ticket =
            ticketReader
                .findWithLinesById(command.tenantId(), command.ticketId())
                .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

        var now = Instant.now(clock);
        ticket.markPaymentPending(now); // domain enforces RESULTED_WIN only

        var saved = ticketWriter.save(ticket);

        // Optional: publish event after commit (if you want listeners / UI refresh)
        var event =
            new TicketPaymentPendingEvent(
                UUID.randomUUID(),
                now,
                command.tenantId(),
                saved.getId(),
                command.reason(),
                command.performedBy()
            );

        AfterCommit.run(() -> publisher.publish(event));
        return saved;
    }
}
