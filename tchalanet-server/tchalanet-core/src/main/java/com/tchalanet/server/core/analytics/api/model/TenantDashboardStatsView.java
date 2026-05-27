package com.tchalanet.server.core.analytics.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Analytics projection for the tenant-admin dashboard.
 *
 * <p>Summary card covers the requested date window.
 * Daily breakdown is ordered ascending by {@code refDate}.
 */
public record TenantDashboardStatsView(
    LocalDate from,
    LocalDate to,
    TenantSummaryCard summary,
    List<TenantDailyPoint> dailyBreakdown,
    List<TenantGameBreakdown> gameBreakdown
) {

  /** Aggregated KPIs for the full window. */
  public record TenantSummaryCard(
      long    ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal payoutsPaid,
      BigDecimal netRevenueEstimated,
      long    sessionsOpened
  ) {}

  /** Single date point for a sparkline/chart. */
  public record TenantDailyPoint(
      LocalDate refDate,
      long      ticketsSold,
      BigDecimal grossSales
  ) {}

  /** Per-game breakdown for the window. */
  public record TenantGameBreakdown(
      String    gameCode,
      long      ticketsSold,
      BigDecimal grossSales
  ) {}
}
