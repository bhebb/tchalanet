package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.core.sales.api.model.sale.SaleActionAvailability;
import com.tchalanet.server.core.sales.api.model.sale.SaleDecision;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import java.util.List;

public record CashierTicketPreviewResponse(
    SaleDecision decision,
    List<SaleIssueView> issues,
    SaleActionAvailability actionAvailability,
    String sellerInstruction,
    String warning
) {
}
