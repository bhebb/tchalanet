package com.tchalanet.server.core.sales.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.event.TicketRejectedEvent;
import com.tchalanet.server.core.sales.internal.application.command.model.RejectTicketSaleCommand;
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
public class RejectTicketSaleCommandHandler implements CommandHandler<RejectTicketSaleCommand, Ticket> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public Ticket handle(RejectTicketSaleCommand cmd) {
        var ctx = TchContext.currentOrThrow();
        var correlationId = ctx.correlationId();
        var ticket = ticketReader.getRequired(cmd.ticketId());

        var now = Instant.now(clock);

        // Domain transition
        var updated = ticket.reject(cmd.rejectedBy(), cmd.reason(), now);

        // Persist
        var saved = ticketWriter.save(updated);

        // Publish event after commit
        AfterCommit.run(() -> {
            var approvalRequest = saved.approvalRequestId();
            publisher.publish(
                new TicketRejectedEvent(
                    EventId.of(idGenerator.newUuid()),
                    TicketRejectedEvent.CURRENT_SCHEMA,
                    now,
                    correlationId,
                    saved.identity().tenantId(),
                    saved.identity().id(),
                    approvalRequest.orElse(null),
                    cmd.rejectedBy(),
                    cmd.reason()
                )
            );
        });

        log.info("Ticket rejected ticketId={} tenantId={}", saved.identity().id(), saved.identity().tenantId());

        return saved;
    }
}

