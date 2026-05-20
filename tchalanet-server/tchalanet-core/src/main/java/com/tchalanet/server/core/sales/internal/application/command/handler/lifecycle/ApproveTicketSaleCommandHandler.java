package com.tchalanet.server.core.sales.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.event.TicketApprovedEvent;
import com.tchalanet.server.core.sales.internal.application.command.model.ApproveTicketSaleCommand;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ApproveTicketSaleCommandHandler implements CommandHandler<ApproveTicketSaleCommand, Ticket> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public Ticket handle(ApproveTicketSaleCommand cmd) {
        var ctx = TchContext.currentOrThrow();
        var correlationId = ctx.correlationId();
        var ticket = ticketReader.getRequired(cmd.ticketId());

        var now = Instant.now(clock);

        // Domain transition: approve the ticket with optional reason
        var updated = ticket.approve(cmd.approvedBy(), cmd.reason(), now);

        // Persist
        var saved = ticketWriter.save(updated);

        // Publish TicketApprovedEvent after commit
        AfterCommit.run(() -> {
            var approvalRequest = saved.approvalRequestId();
            publisher.publish(
                new TicketApprovedEvent(
                    EventId.of(idGenerator.newUuid()),
                    TicketApprovedEvent.CURRENT_SCHEMA,
                    now,
                    correlationId,
                    saved.identity().tenantId(),
                    saved.identity().id(),
                    approvalRequest.orElse(null),
                    cmd.approvedBy(),
                    cmd.reason()
                )
            );
        });

        log.info("Ticket approved ticketId={} tenantId={}", saved.identity().id(), saved.identity().tenantId());

        return saved;
    }
}

