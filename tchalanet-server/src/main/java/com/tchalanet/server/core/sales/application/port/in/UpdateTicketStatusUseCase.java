package com.tchalanet.server.core.sales.application.port.in;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.UUID;

/** Input port for updating ticket status (used by controllers). */
public interface UpdateTicketStatusUseCase {

  Ticket markAsPaid(UpdateStatusCommand command);

  Ticket voidTicket(UpdateStatusCommand command);

  record UpdateStatusCommand(UUID tenantId, UUID ticketId, UUID userId, boolean isAdmin) {}
}

