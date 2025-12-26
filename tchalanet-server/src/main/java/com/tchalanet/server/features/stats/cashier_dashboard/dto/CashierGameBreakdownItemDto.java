package com.tchalanet.server.features.stats.cashier_dashboard.dto;

import java.math.BigDecimal;

public record CashierGameBreakdownItemDto(
    String gameCode,
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue) {}
