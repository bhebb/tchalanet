package com.tchalanet.server.core.analytics.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row in a sales report: daily, per-game breakdown.
 */
public record SalesReportLine(
    LocalDate  date,
    String     gameCode,
    long       ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue
) {}
