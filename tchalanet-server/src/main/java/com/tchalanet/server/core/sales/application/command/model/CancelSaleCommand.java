package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

/**
 * Command to cancel a sale / ticket.
 */
public record CancelSaleCommand(
    TenantId tenantId,
    TicketId ticketId,
    UserId performedBy,
    String reason,
    String currency
) implements Command<CancelSaleResult> {
}
