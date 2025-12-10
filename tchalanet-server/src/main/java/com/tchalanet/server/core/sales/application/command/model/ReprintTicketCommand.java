package com.tchalanet.server.core.sales.application.command.model;

import java.util.UUID;

/** Command to request a reprint of a ticket. */
public record ReprintTicketCommand(
    UUID tenantId,
    UUID ticketId,
    UUID userId
) {}

