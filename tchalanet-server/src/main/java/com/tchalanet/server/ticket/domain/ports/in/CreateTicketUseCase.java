package com.tchalanet.server.ticket.domain.ports.in;

import com.tchalanet.server.ticket.domain.model.Ticket;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Inbound Port for creating a new ticket. */
public interface CreateTicketUseCase {

  Ticket createTicket(CreateTicketCommand command);

  record CreateTicketCommand(
      UUID tenantId, UUID terminalId, UUID drawId, List<LineCommand> lines) {}

  record LineCommand(String gameCode, String selection, BigDecimal stake) {}
}
