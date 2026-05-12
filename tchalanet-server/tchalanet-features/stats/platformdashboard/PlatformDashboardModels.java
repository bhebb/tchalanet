package com.tchalanet.server.features.stats.platformdashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PlatformDashboardModels {
  public record PlatformDashboardStatsCriteria(LocalDate fromDate, LocalDate toDate) {}

  public record PlatformSummaryCard(
      long totalTenants,
      long totalOutlets,
      long totalCashiers,
      long totalTickets,
      long totalStakeCents,
      long totalWinningsCents,
      long totalNetRevenueCents) {}

  public record PlatformTenantStats(
      UUID tenantId,
      String tenantName,
      long ticketsCount,
      long stakeSumCents,
      long netRevenueCents) {}

  public record PlatformDailySalesPoint(
      LocalDate refDate, long stakeSumCents, long winningsSumCents, long netRevenueCents) {}

  public record PlatformCashierStats(
      UUID cashierId,
      String cashierDisplayName,
      long ticketsCount,
      long stakeSumCents,
      long netRevenueCents) {}

  public record PlatformGameBreakdownItem(
      String gameCode, long ticketsCount, long stakeSumCents, long netRevenueCents) {}

  public record PlatformDashboardStatsResponse(
      LocalDate fromDate,
      LocalDate toDate,
      PlatformSummaryCard summary,
      List<PlatformTenantStats> tenants,
      List<PlatformDailySalesPoint> dailySales,
      List<PlatformGameBreakdownItem> gameBreakdown,
      List<PlatformCashierStats> topCashiers) {}
}
