package com.tchalanet.server.features.stats.cashierdashboard.model;

import java.time.LocalDate;
import java.util.List;

public record CashierDashboardStatsResponse(
    LocalDate fromDate,
    LocalDate toDate,
    CashierSummaryCard summary,
    List<CashierGameBreakdownItem> gameBreakdown,
    List<CashierDailySalesPoint> dailySales) {}
