package com.tchalanet.server.core.sales.api.query.preview;

import com.tchalanet.server.core.sales.api.model.sale.SaleActionAvailability;
import com.tchalanet.server.core.sales.api.model.sale.SaleDecision;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import java.util.List;
import java.util.Objects;

public record TicketSalePreviewResult(
    SaleDecision decision,
    List<SaleIssueView> issues,
    SaleActionAvailability actionAvailability,
    String sellerInstruction,
    String warning
) {
    public TicketSalePreviewResult {
        Objects.requireNonNull(decision, "decision is required");
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
