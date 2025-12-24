package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;

import java.util.UUID;

/**
 * Command to cancel a sale / ticket.
 */
public record CancelSaleCommand(
    UUID tenantId,
    UUID ticketId,
    UUID performedBy,
    String reason,
    String currency
) implements Command<CancelSaleResult> {
}
