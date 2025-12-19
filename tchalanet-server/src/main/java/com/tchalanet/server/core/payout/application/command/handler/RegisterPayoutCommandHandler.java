package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutCommand;
import com.tchalanet.server.core.payout.port.out.PayoutRepositoryPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutApprovalPolicyPort;
import com.tchalanet.server.core.payout.domain.event.PayoutRegisteredEvent;
import com.tchalanet.server.core.payout.domain.model.Payout;
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
public class RegisterPayoutCommandHandler implements CommandHandler<RegisterPayoutCommand, Payout> {

  private final PayoutRepositoryPort payoutRepository;
  private final TicketReaderPort ticketReaderPort;
  private final TicketWritterPort ticketWritterPort;
  private final PayoutApprovalPolicyPort approvalPolicy;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public Payout handle(RegisterPayoutCommand command) {
    Instant now = Instant.now(clock);
    // Load ticket
    Optional<Ticket> optTicket = ticketReaderPort.findById(command.ticketId());
    if (optTicket.isEmpty()) {
      throw new IllegalStateException("Ticket not found: " + command.ticketId());
    }
    Ticket ticket = optTicket.get();

    // Validate ticket state
    if (ticket.getStatus() != com.tchalanet.server.core.sales.domain.model.TicketStatus.RESULTED_WIN) {
      throw new IllegalStateException("Ticket is not in RESULTED_WIN state: " + ticket.getId());
    }

    // Validate not already paid
    if (payoutRepository.findByTicketId(ticket.getId()).isPresent()) {
      throw new IllegalStateException("Payout already registered for ticket: " + ticket.getId());
    }

    // Create payout and persist
    Payout payout = Payout.createRequested(command.tenantId(), command.ticketId(), command.amount(), now);
    Payout saved = payoutRepository.save(payout);

    // Decide auto-approval via policy
    boolean autoApprove = approvalPolicy.autoApprove(command.tenantId(), command.amount());
    if (autoApprove) {
      saved.markPaid(now);
      saved = payoutRepository.save(saved);

      // mark ticket paid and persist
      ticket.markAsPaid(now);
      ticketWritterPort.save(ticket);
    } else {
      // approve or leave requested based on workflow; for now mark as APPROVED
      saved.approve(now);
      saved = payoutRepository.save(saved);
    }

    // Publish event
    PayoutRegisteredEvent event = new PayoutRegisteredEvent(
        UUID.randomUUID(),
        now,
        new com.tchalanet.server.core.tenant.domain.model.TenantId(command.tenantId()),
        saved.getId(),
        saved.getTicketId(),
        saved.getAmount());

    domainEventPublisher.publish(event);

    log.info("Payout {} registered with status={} for ticket {}", saved.getId(), saved.getStatus(), saved.getTicketId());
    return saved;
  }
}
