package com.tchalanet.server.core.sales.application;

import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.ports.in.UpdateTicketStatusUseCase;
import com.tchalanet.server.core.sales.domain.ports.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateTicketStatusService implements UpdateTicketStatusUseCase {

  private final TicketRepositoryPort ticketRepository;

  // private final AccessCheckerPort accessChecker; // No longer directly injected here, handled by
  // annotation
  // private final DrawReadModelPort drawReadModel; // To check if draw is passed

  @Override
  @RequiresPermission("ticket.mark_paid") // Apply the annotation
  public Ticket markAsPaid(UpdateStatusCommand command) {
    Ticket ticket = findAndAuthorize(command);
    ticket.markAsPaid(); // Business logic is in the domain model
    return ticketRepository.save(ticket);
  }

  @Override
  @RequiresPermission("ticket.void") // Apply the annotation
  public Ticket voidTicket(UpdateStatusCommand command) {
    Ticket ticket = findAndAuthorize(command);

    // Example of a rule that lives in the use case because it involves external data
    // boolean isDrawTimePassed = drawReadModel.isDrawTimePassed(ticket.getDrawId());
    // if (isDrawTimePassed && !command.isAdmin()) {
    //     throw new SecurityException("Cannot void a ticket after the draw has started.");
    // }

    ticket.voidTicket(); // Business logic is in the domain model
    return ticketRepository.save(ticket);
  }

  private Ticket findAndAuthorize(UpdateStatusCommand command) {
    Ticket ticket =
        ticketRepository
            .findById(command.ticketId())
            .orElseThrow(
                () -> new IllegalArgumentException("Ticket not found: " + command.ticketId()));

    if (!ticket.getTenantId().equals(command.tenantId())) {
      throw new SecurityException("Tenant mismatch for ticket " + command.ticketId());
    }
    return ticket;
  }
}
