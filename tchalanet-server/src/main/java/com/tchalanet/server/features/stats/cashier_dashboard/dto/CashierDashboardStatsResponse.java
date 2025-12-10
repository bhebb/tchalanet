package com.tchalanet.server.features.stats.cashier_dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record CashierDashboardStatsResponse(
    LocalDate fromDate,
    LocalDate toDate,
    CashierSummaryCardDto summary,
    List<CashierGameBreakdownItemDto> gameBreakdown,
    List<CashierDailySalesPointDto> dailySales
) {}
