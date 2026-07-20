package com.tchalanet.server.core.sales.api.model.analytics;

import java.math.BigDecimal;

/** Immutable ticket-line data required for analytics reconciliation. */
public record SalesAnalyticsTicketLineSnapshot(
    String gameCode,
    String betType,
    Short betOption,
    String selectionKey,
    BigDecimal stakeAmount,
    BigDecimal payoutAmount,
    String origin,
    String pricingSource) {}
