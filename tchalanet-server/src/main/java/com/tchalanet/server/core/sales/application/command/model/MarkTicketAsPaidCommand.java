package com.tchalanet.server.core.sales.application.command.model;

import java.util.UUID;

/** Command to mark a ticket as paid. */
public record MarkTicketAsPaidCommand(
    UUID tenantId,
    UUID ticketId,
    UUID userId,
    boolean isAdmin
) {}
