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
    List<TenantRankRow> topTenants
) {

  /** Global rollup across all tenants for the window. */
  public record PlatformSummaryCard(
      long       totalTenants,
      long       ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
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
