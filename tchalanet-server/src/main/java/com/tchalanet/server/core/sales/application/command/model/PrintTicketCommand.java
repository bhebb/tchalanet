package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;

import com.tchalanet.server.common.bus.Command;

import java.util.UUID;

/**
 * Command to print a ticket.
 */
public record PrintTicketCommand(
    TicketId ticketId
) implements Command<String> {
}
