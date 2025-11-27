package com.tchalanet.server.ticket.domain.ports.in;

import com.tchalanet.server.ticket.domain.model.Ticket;
import java.util.UUID;

/** Inbound Port for updating the status of a ticket. */
public interface UpdateTicketStatusUseCase {

  /**
   * Marks a ticket as PAID.
   *
   * @param command The command containing the ticket and user IDs.
   * @return The updated ticket.
   */
  Ticket markAsPaid(UpdateStatusCommand command);

  /**
   * Voids a ticket, making it invalid.
   *
   * @param command The command containing the ticket and user IDs.
   * @return The updated ticket.
   */
  Ticket voidTicket(UpdateStatusCommand command);

  record UpdateStatusCommand(
      UUID tenantId,
      UUID ticketId,
      UUID userId, // The user performing the action
      boolean isAdmin // To allow overriding certain rules
      ) {}
}
