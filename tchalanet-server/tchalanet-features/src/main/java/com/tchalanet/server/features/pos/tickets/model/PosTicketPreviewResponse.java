package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.core.sales.api.model.sale.SaleActionAvailability;
import com.tchalanet.server.core.sales.api.model.sale.SaleDecision;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import java.util.List;

public record PosTicketPreviewResponse(
    SaleDecision decision,
    List<SaleIssueView> issues,
    SaleActionAvailability actionAvailability,
    String sellerInstruction,
    String warning
) {
}
