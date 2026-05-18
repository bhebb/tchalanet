package com.tchalanet.server.core.sales.api.query;

import java.time.Instant;

public record CashierPendingApprovalView(
    String publicCode,
    long stakeTotalCents,
    String drawLabel,
    Instant submittedAt
) {}
