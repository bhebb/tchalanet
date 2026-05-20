package com.tchalanet.server.core.sales.api.query;

import java.time.Instant;

/**
 * Public-safe view of a recent ticket for the cashier dashboard widget.
 * Does not expose internal IDs (TicketId, DrawId, TenantId).
 */
public record CashierRecentTicketView(
    String publicCode,
    String statusCode,
    Instant soldAt,
    long stakeTotalCents,
    long potentialPayoutCents,
    String drawLabel,
    int lineCount
) {}
