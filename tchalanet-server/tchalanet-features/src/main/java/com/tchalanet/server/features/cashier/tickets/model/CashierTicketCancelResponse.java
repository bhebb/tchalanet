package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.command.cancel.CancelTicketResult;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import java.time.Instant;
import java.util.List;

public record CashierTicketCancelResponse(
    TicketId ticketId,
    CancelTicketResult.CancelTicketOutcome outcome,
    Instant cancelledAt,
    List<SaleIssueView> issues
) {
}
