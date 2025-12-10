package com.tchalanet.server.core.sales.application.command.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Command to create a new ticket. */
public record CreateTicketCommand(
    UUID tenantId,
    UUID terminalId,
    UUID drawId,
    List<LineCommand> lines
) {

  /** Command for a ticket line. */
  public record LineCommand(String gameCode, String selection, BigDecimal stake) {}
}
