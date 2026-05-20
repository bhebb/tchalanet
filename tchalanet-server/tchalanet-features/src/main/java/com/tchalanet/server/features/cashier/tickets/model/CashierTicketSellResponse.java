package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.model.sale.SaleActionAvailability;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import com.tchalanet.server.core.sales.api.model.sale.TicketBackupInfo;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.util.List;

public record CashierTicketSellResponse(
    SellTicketOutcome outcome,
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    TicketSaleStatus saleStatus,
    List<SaleIssueView> issues,
    TicketBackupInfo backup,
    SaleActionAvailability actionAvailability,
    String sellerInstruction
) {
}
