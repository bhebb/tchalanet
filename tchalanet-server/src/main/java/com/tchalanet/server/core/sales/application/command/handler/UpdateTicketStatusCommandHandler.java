package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.application.port.in.UpdateTicketStatusUseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateTicketStatusCommandHandler implements UpdateTicketStatusUseCase {

  private final TicketWritterPort ticketRepository;

  @Override
  @TchTx
  @RequiresPermission("ticket.mark_paid")
  public Ticket markAsPaid(UpdateTicketStatusUseCase.UpdateStatusCommand command) {
    Ticket ticket = findAndAuthorize(command.tenantId(), command.ticketId());
    ticket.markAsPaid();
    return ticketRepository.save(ticket);
  }

  @Override
  @TchTx
  @RequiresPermission("ticket.void")
  public Ticket voidTicket(UpdateTicketStatusUseCase.UpdateStatusCommand command) {
    Ticket ticket = findAndAuthorize(command.tenantId(), command.ticketId());
    ticket.voidTicket();
    return ticketRepository.save(ticket);
  }

  private Ticket findAndAuthorize(java.util.UUID tenantId, java.util.UUID ticketId) {
    Ticket ticket =
        ticketRepository
            .findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    if (!ticket.getTenantId().equals(tenantId)) {
      throw new SecurityException("Tenant mismatch for ticket " + ticketId);
    }
    return ticket;
  }
}

