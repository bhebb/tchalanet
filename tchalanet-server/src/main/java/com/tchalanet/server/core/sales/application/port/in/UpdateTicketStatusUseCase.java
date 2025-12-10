package com.tchalanet.server.core.sales.application.port.in;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.UUID;

/** Inbound use case to update ticket status (mark paid / void). */
public interface UpdateTicketStatusUseCase {

  /** Command to update status. */
  public record UpdateStatusCommand(UUID tenantId, UUID ticketId, UUID userId, boolean isAdmin) {}

  Ticket markAsPaid(UpdateStatusCommand command);

  Ticket voidTicket(UpdateStatusCommand command);
}

