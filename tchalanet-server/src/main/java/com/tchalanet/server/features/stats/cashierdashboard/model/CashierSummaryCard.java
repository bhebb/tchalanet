package com.tchalanet.server.features.stats.cashierdashboard.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CashierSummaryCard(
    UUID cashierId,
    String cashierName,
    UUID outletId,
    String outletName,
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue,
    boolean hasOpenSession) {}
