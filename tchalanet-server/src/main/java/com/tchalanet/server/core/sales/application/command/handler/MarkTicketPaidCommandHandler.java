package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.sales.application.command.model.MarkTicketPaidCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketPaidEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
@Secured("ticket.mark_paid")  // Assuming @RequiresPermission maps to @Secured or similar
public class MarkTicketPaidCommandHandler implements CommandHandler<MarkTicketPaidCommand, Ticket> {

    private final TicketReaderPort ticketReader;
    private final TicketWritterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    public Ticket handle(MarkTicketPaidCommand cmd) {
        var ticket = ticketReader.findWithLinesById(cmd.tenantId(), cmd.ticketId())
            .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

        var now = Instant.now(clock);
        ticket.markAsPaid(now);
        var saved = ticketWriter.save(ticket);

        long totalAmountCents = saved.getTotalAmount().movePointRight(2).longValue();

        var event = new TicketPaidEvent(
            UUID.randomUUID(),
            now,
            new TenantId(saved.getTenantId()),
            saved.getId(),
            cmd.performedBy(),
            cmd.reason(),
            totalAmountCents,
            "USD"  // TODO: get from tenant or command
        );

        AfterCommit.run(() -> publisher.publish(event));
        log.info("Ticket marked as paid ticketId={} tenantId={}", saved.getId(), saved.getTenantId());
        return saved;
    }
}
