package com.tchalanet.server.features.stats.tenantdashboard.model;

import java.time.LocalDate;
import java.util.List;

public record TenantDashboardStatsView(
    LocalDate fromDate,
    LocalDate toDate,
    TenantSummaryCard summary,
    List<TenantGameBreakdownItem> gameBreakdown,
    List<TenantDailySalesPoint> dailySales) {}
