package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.payout.application.command.model.ExecutePayoutCommand;
import com.tchalanet.server.core.payout.port.out.PayoutRepositoryPort;
import com.tchalanet.server.core.payout.domain.event.PayoutRegisteredEvent;
import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ExecutePayoutCommandHandler implements CommandHandler<ExecutePayoutCommand, Payout> {

  private final PayoutRepositoryPort payoutRepository;
  private final TicketReaderPort ticketReaderPort;
  private final TicketWritterPort ticketWritterPort;
  private final LedgerWriterPort ledgerWriter;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public Payout handle(ExecutePayoutCommand command) {
    Optional<Payout> optPayout = payoutRepository.findById(command.payoutId());
    if (optPayout.isEmpty()) {
      throw new IllegalStateException("Payout not found: " + command.payoutId());
    }
    Payout payout = optPayout.get();

    if (payout.getStatus() == PayoutStatus.PAID) {
      // idempotent: already paid
      return payout;
    }

    if (payout.getStatus() != PayoutStatus.APPROVED && payout.getStatus() != PayoutStatus.REQUESTED) {
      throw new IllegalStateException("Payout is not approved for execution: " + payout.getId());
    }

    // load ticket and validate
    Optional<Ticket> optTicket = ticketReaderPort.findById(payout.getTicketId());
    if (optTicket.isEmpty()) {
      throw new IllegalStateException("Ticket not found for payout: " + payout.getTicketId());
    }
    Ticket ticket = optTicket.get();
    if (ticket.getStatus() != com.tchalanet.server.core.sales.domain.model.TicketStatus.WON) {
      throw new IllegalStateException("Ticket is not in WON/RESULTED_WIN state: " + ticket.getId());
    }

    // execute payout
    Instant now = Instant.now(clock);
    payout.markPaid(now);
    Payout saved = payoutRepository.save(payout);

    // mark ticket paid and persist
    ticket.markAsPaid(now);
    ticketWritterPort.save(ticket);

    // ledger entry
    LedgerEntry entry = LedgerEntry.create(
        saved.getTenantId(),
        "PAYOUT",
        saved.getId(),
        saved.getAmount(),
        "DEBIT");
    ledgerWriter.append(entry);

    // publish event
    PayoutRegisteredEvent event = new PayoutRegisteredEvent(
        UUID.randomUUID(),
        Instant.now(clock),
        new com.tchalanet.server.core.tenant.domain.model.TenantId(saved.getTenantId()),
        saved.getId(),
        saved.getTicketId(),
        saved.getAmount());
    domainEventPublisher.publish(event);

    log.info("Executed payout {} for ticket {}", saved.getId(), saved.getTicketId());
    return saved;
  }
}
