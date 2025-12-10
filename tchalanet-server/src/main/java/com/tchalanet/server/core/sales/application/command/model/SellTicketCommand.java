package com.tchalanet.server.core.sales.application.command.model;

import java.util.List;
import java.util.UUID;

/** Command to sell a ticket (purchase). */
public record SellTicketCommand(
    UUID tenantId,
    UUID terminalId,
    UUID drawId,
    UUID userId,
    List<CreateTicketCommand.LineCommand> lines
) {}

