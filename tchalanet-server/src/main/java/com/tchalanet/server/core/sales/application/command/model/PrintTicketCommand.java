package com.tchalanet.server.core.sales.application.command.model;

import java.util.UUID;

/** Command to print a ticket. */
public record PrintTicketCommand(
    UUID ticketId,
    UUID tenantId
) {}
