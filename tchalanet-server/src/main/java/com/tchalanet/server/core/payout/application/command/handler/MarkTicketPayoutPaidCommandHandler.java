package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.payout.application.command.model.MarkTicketPayoutPaidCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.event.TicketPaidEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class MarkTicketPayoutPaidCommandHandler implements CommandHandler<MarkTicketPayoutPaidCommand, Ticket> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    @RequiresPermission("ticket.payout.mark_paid")
    public Ticket handle(MarkTicketPayoutPaidCommand cmd) {
        Ticket ticket = ticketReader.findWithLinesById(cmd.ticketId())
            .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

        Instant now = Instant.now(clock);

        ticket.settle(now);

        Ticket saved = ticketWriter.save(ticket);

        AfterCommit.run(() -> publisher.publish(
            new TicketPaidEvent(
                EventId.of(UUID.randomUUID()),
                now,
                saved.tenantId(),
                saved.id(),
                cmd.performedBy(),
                cmd.reason(),
                // reflect amount paid: should come from payout domain in the future
                saved.totalPayout().movePointRight(2).longValue(),
                cmd.currency() // put currency in command; no hardcode
            )
        ));

        log.info("Ticket payout marked PAID ticketId={} tenantId={}", saved.id(), saved.tenantId());
        return saved;
    }
}
