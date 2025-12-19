package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
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
  private final TicketWritterPort ticketWritter;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public Ticket handle(RecordTicketResultCommand command) {
    Optional<Ticket> opt = ticketReader.findById(command.ticketId());
    if (opt.isEmpty()) {
      throw new IllegalStateException("Ticket not found: " + command.ticketId());
    }
    Ticket ticket = opt.get();

    // Compute total payout from ticket lines (using oddsSnapshot/potentialPayout)
    BigDecimal totalPayout = BigDecimal.ZERO;
    for (TicketLine line : ticket.getLines()) {
      BigDecimal p = line.potentialPayout();
      if (p != null) totalPayout = totalPayout.add(p);
    }

    // Apply result to domain
    ticket.recordResult(totalPayout, Instant.now(clock));

    // Persist updated ticket
    Ticket saved = ticketWritter.save(ticket);

    // Publish domain event
    TicketResultedEvent event = new TicketResultedEvent(
        UUID.randomUUID(),
        Instant.now(clock),
        new com.tchalanet.server.core.tenant.domain.model.TenantId(command.tenantId()),
        saved.getId(),
        totalPayout);

    domainEventPublisher.publish(event);

    log.info("Recorded result for ticket {} => payout {}", saved.getId(), totalPayout);
    return saved;
  }
}
