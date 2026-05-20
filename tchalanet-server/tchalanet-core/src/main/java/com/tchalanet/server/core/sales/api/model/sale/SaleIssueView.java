package com.tchalanet.server.core.sales.api.model.sale;

import java.util.Map;
import java.util.Objects;

public record SaleIssueView(
    String code,
    SaleIssueSeverity severity,
    String message,
    String sellerInstruction,
    int lineIndex,
    Map<String, Object> details
) {
    public SaleIssueView {
        Objects.requireNonNull(code, "code is required");
        Objects.requireNonNull(severity, "severity is required");
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static SaleIssueView basket(
        String code,
        SaleIssueSeverity severity,
        String message,
        String sellerInstruction,
        Map<String, Object> details
    ) {
        return new SaleIssueView(code, severity, message, sellerInstruction, -1, details);
    }
}
