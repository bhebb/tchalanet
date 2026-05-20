package com.tchalanet.server.features.reporting.outletperformance;

import java.math.BigDecimal;
import java.util.UUID;

public record OutletPerformanceLine(
    UUID outletId,
    String outletCode,
    String outletName,
    String gameCode, // null si tous jeux
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue) {}
