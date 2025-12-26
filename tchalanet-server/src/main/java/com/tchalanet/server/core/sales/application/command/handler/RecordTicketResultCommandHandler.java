package com.tchalanet.server.core.sales.application.command.handler;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.sales.application.command.model.RecordTicketResultCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.domain.event.TicketResultedEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordTicketResultCommandHandler implements CommandHandler<RecordTicketResultCommand, Ticket> {

    private final TicketReaderPort ticketReader;
    private final TicketWritterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    public Ticket handle(RecordTicketResultCommand command) {
        var ticket = ticketReader.findWithLinesById(command.ticketId())
            .orElseThrow(() -> new IllegalStateException("Ticket not found"));

        var now = Instant.now(clock);
        ticket.recordResult(command.winningAmount(), now);

        var saved = ticketWriter.save(ticket);

        var event = new TicketResultedEvent(
            UUID.randomUUID(),
            now,
            saved.getTenantId(),
            saved.getId(),
            command.winningAmount()
        );

        AfterCommit.run(() -> publisher.publish(event));
        return saved;
    }
}
