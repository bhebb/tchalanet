package com.tchalanet.server.core.analytics.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Analytics projection for the platform / super-admin dashboard.
 *
 * <p>Platform-level aggregation across all tenants.
 * Per-tenant breakdown enables the top-N tenants widget.
 */
public record PlatformDashboardStatsView(
    LocalDate from,
    LocalDate to,
    PlatformSummaryCard summary,
    List<PlatformDailyPoint> dailyBreakdown,
    List<PlatformGameBreakdown> gameBreakdown,
    List<TenantRankRow> topTenants
) {

  /** Global rollup across all tenants for the window. */
  public record PlatformSummaryCard(
      long       totalTenants,
      long       ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal payoutsPaid,
      BigDecimal sellerCommission,
      BigDecimal tenantCharges,
      long promotionLines,
      BigDecimal promotionPotentialPayout,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis
  ) {}

  /** Single date point for trend widgets and platform reports. */
  public record PlatformDailyPoint(
      LocalDate  refDate,
      long       ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal payoutsPaid,
      BigDecimal sellerCommission,
      BigDecimal tenantCharges,
      long promotionLines,
      BigDecimal promotionPotentialPayout,
      BigDecimal netRevenueEstimated,
      BigDecimal netRevenuePaidBasis
  ) {}

  /** Per-game platform breakdown for the requested window (populated once GAME rows exist). */
  public record PlatformGameBreakdown(
      String     gameCode,
      String     gameLabel,
      long       ticketsSold,
      BigDecimal grossSales,
      BigDecimal netRevenueEstimated
  ) {}

  /** One row in the top-tenant ranking. */
  public record TenantRankRow(
      String     tenantCode,
      long       ticketsSold,
      BigDecimal grossSales,
      BigDecimal netRevenueEstimated
  ) {}
}
