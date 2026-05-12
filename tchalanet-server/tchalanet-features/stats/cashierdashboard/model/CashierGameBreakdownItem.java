package com.tchalanet.server.features.stats.cashierdashboard.model;

import java.math.BigDecimal;

public record CashierGameBreakdownItem(
    String gameCode,
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue) {}
