package com.tchalanet.server.features.stats.cashier_dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CashierSummaryCardDto(
    UUID cashierId,
    String cashierName,
    UUID outletId,
    String outletName,
    long ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue,
    boolean hasOpenSession
) {}
