package com.tchalanet.server.core.sales.api.model.analytics;

import java.math.BigDecimal;

/** Immutable ticket-charge data required for analytics reconciliation. */
public record SalesAnalyticsTicketChargeSnapshot(
    String paidBy, BigDecimal amount, boolean waived) {}
