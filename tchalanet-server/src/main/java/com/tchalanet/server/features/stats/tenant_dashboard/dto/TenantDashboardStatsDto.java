package com.tchalanet.server.features.stats.tenant_dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record TenantDashboardStatsDto(
    LocalDate fromDate,
    LocalDate toDate,
    TenantSummaryCardDto summary,
    List<TenantGameBreakdownItemDto> gameBreakdown,
    List<TenantDailySalesPointDto> dailySales) {}
