package com.tchalanet.server.core.sales.internal.application.command.handler.print;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.command.print.RecordTicketPrintCommand;
import com.tchalanet.server.core.sales.api.command.print.RecordTicketPrintResult;
import com.tchalanet.server.core.sales.api.event.TicketPrintedEvent;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.print.TicketPrintPolicyService;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class RecordTicketPrintCommandHandler
    implements CommandHandler<RecordTicketPrintCommand, RecordTicketPrintResult> {

    private final TicketReaderPort reader;
    private final TicketWriterPort writer;
    private final DomainEventPublisher events;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final TicketPrintPolicyService printPolicy;

    @Override
    @TchTx
    public RecordTicketPrintResult handle(RecordTicketPrintCommand command) {
        var now = Instant.now(clock);
        var context = TchContext.currentOrThrow();
        var ticket = reader.getRequired(command.ticketId());
        printPolicy.requirePrintAllowed(ticket, command);
        ticket = ticket.markPrinted(context.userId(), now);

        Ticket saved = writer.save(ticket);

        AfterCommit.run(() -> events.publish(
            TicketPrintedEvent.from(
                EventId.of(idGenerator.newUuid()),
                saved,
                command.format(),
                command.reason(),
                now)
        ));

        return new RecordTicketPrintResult(saved.identity().id(), saved.print());
    }
}
