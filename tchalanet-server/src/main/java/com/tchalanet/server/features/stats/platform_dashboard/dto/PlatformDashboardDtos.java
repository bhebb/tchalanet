package com.tchalanet.server.features.stats.platform_dashboard.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PlatformDashboardDtos {
  public record PlatformDashboardStatsQuery(LocalDate fromDate, LocalDate toDate) {}

  public record PlatformSummaryCardDto(
      long totalTenants,
      long totalOutlets,
      long totalCashiers,
      long totalTickets,
      long totalStakeCents,
      long totalWinningsCents,
      long totalNetRevenueCents) {}

  public record PlatformTenantStatsDto(
      UUID tenantId,
      String tenantName,
      long ticketsCount,
      long stakeSumCents,
      long netRevenueCents) {}

  public record PlatformDailySalesPointDto(
      LocalDate refDate, long stakeSumCents, long winningsSumCents, long netRevenueCents) {}

  public record PlatformCashierStatsDto(
      UUID cashierId,
      String cashierDisplayName,
      long ticketsCount,
      long stakeSumCents,
      long netRevenueCents) {}

  public record PlatformGameBreakdownItemDto(
      String gameCode, long ticketsCount, long stakeSumCents, long netRevenueCents) {}

  public record PlatformDashboardStatsResponse(
      LocalDate fromDate,
      LocalDate toDate,
      PlatformSummaryCardDto summary,
      List<PlatformTenantStatsDto> tenants,
      List<PlatformDailySalesPointDto> dailySales,
      List<PlatformGameBreakdownItemDto> gameBreakdown,
      List<PlatformCashierStatsDto> topCashiers) {}
}
