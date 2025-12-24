package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

/**
 * Command to print a ticket.
 */
public record PrintTicketCommand(
    UUID ticketId
) implements Command<String> {}
