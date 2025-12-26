package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TicketId;

/** Command to print a ticket. */
public record PrintTicketCommand(TicketId ticketId) implements Command<String> {}
