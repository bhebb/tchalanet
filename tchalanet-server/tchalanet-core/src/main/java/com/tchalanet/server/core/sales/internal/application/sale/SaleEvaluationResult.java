package com.tchalanet.server.core.sales.internal.application.sale;

import com.tchalanet.server.core.sales.api.model.sale.SaleActionAvailability;
import com.tchalanet.server.core.sales.api.model.sale.SaleDecision;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.PreparedSale;

import java.util.List;
import java.util.Objects;

public record SaleEvaluationResult(
    SaleEvaluationMode mode,
    SaleDecision decision,
    PreparedSale preparedSale,
    List<SaleIssueView> issues,
    SaleActionAvailability actionAvailability,
    String sellerInstruction,
    String warning
) {
    public SaleEvaluationResult {
        Objects.requireNonNull(mode, "mode is required");
        Objects.requireNonNull(decision, "decision is required");
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean acceptable() {
        return decision == SaleDecision.ACCEPTABLE;
    }
}
