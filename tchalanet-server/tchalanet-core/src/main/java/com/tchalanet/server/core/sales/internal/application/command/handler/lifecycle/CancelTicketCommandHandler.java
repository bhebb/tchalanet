package com.tchalanet.server.core.sales.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.internal.application.command.model.CancelTicketCommand;
import com.tchalanet.server.core.sales.internal.application.command.model.CancelTicketResult;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelTicketCommandHandler implements CommandHandler<CancelTicketCommand, CancelTicketResult> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public CancelTicketResult handle(CancelTicketCommand cmd) {
        TchContext.currentOrThrow();
        Ticket ticket = ticketReader.getRequired(cmd.ticketId());

        Instant now = Instant.now(clock);

        // Domain transition
        Ticket updated = ticket.cancel(cmd.cancelledBy(), cmd.reason(), now);

        // Persist
        var saved = ticketWriter.save(updated);

        // Publish event after commit
        AfterCommit.run(() -> publisher.publish(
            new TicketCancelledEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                saved.identity().tenantId(),
                saved.identity().id(),
                cmd.cancelledBy(),
                cmd.reason()
            )
        ));

        log.info("Ticket cancelled ticketId={} tenantId={}", saved.identity().id(), saved.identity().tenantId());

        return new CancelTicketResult(saved, CancelTicketResult.CancelOutcome.SUCCESS, List.of());
    }
}

