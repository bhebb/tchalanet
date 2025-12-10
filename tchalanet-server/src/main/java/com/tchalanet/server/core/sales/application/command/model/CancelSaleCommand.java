package com.tchalanet.server.core.sales.application.command.model;

import java.util.UUID;

public record CancelSaleCommand(
/** Command to cancel a sale / ticket. */


boolean isAdmin,
UUID userId,
UUID ticketId,
UUID tenantId) {
}

