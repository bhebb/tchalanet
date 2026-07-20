package com.tchalanet.server.core.analytics.api.model;

import java.time.LocalDate;
import java.util.UUID;

/** One exact source-versus-projection discrepancy found during reconciliation. */
public record AnalyticsReconciliationMismatch(
    String projection,
    LocalDate businessDate,
    UUID drawId,
    UUID sellerTerminalId,
    AnalyticsMetricSnapshot expected,
    AnalyticsMetricSnapshot observed) {}
