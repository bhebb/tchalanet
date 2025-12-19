package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Command to create a new ticket. */
public record CreateTicketCommand(
    UUID tenantId,
    UUID terminalId,
    UUID drawId,
    List<LineCommand> lines
) implements Command<Ticket> {

  /** Command for a ticket line. */
  public record LineCommand(String gameCode, String selection, BigDecimal stake) {}
}
