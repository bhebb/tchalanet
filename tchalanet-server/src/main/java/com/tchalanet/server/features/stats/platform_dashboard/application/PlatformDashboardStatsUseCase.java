package com.tchalanet.server.features.stats.platform_dashboard.application;

import com.tchalanet.server.core.accesscontrol.infra.persistence.TenantUserRepository;
import com.tchalanet.server.core.outlet.infra.persistence.OutletSpringRepository;
import com.tchalanet.server.core.tenant.infra.persistence.TenantJpaRepository;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyJpaRepository;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawJpaRepository;
import com.tchalanet.server.features.stats.platform_dashboard.dto.PlatformDashboardDtos.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlatformDashboardStatsUseCase {

  private final StatsDailyJpaRepository statsDailyRepo;
  private final StatsDrawJpaRepository statsDrawRepo;
  private final TenantJpaRepository tenantRepo;
  private final OutletSpringRepository outletRepo;
  private final TenantUserRepository tenantUserRepo;

  @Transactional(readOnly = true)
  public PlatformDashboardStatsResponse handle(PlatformDashboardStatsQuery query) {
    LocalDate from = query.fromDate();
    LocalDate to = query.toDate();

    // 1) platform daily rows
    List<StatsDailyEntity> platformRows =
        statsDailyRepo.findByDimensionTypeAndDimensionIdAndRefDateBetween(
            "platform", null, from, to);

    long totalTickets = 0L;
    long totalStake = 0L;
    long totalWinnings = 0L;
    long totalNet = 0L;

    List<PlatformDailySalesPointDto> dailyPoints = new ArrayList<>();
    for (StatsDailyEntity row : platformRows) {
      totalTickets += row.getTicketsCount();
      totalStake += row.getStakeSumCents();
      totalWinnings += row.getWinningsSumCents();
      totalNet += row.getNetRevenueCents();

      dailyPoints.add(
          new PlatformDailySalesPointDto(
              row.getRefDate(),
              row.getStakeSumCents(),
              row.getWinningsSumCents(),
              row.getNetRevenueCents()));
    }

    // 2) tenant aggregation across the period
    // fetch tenant rows in range
    var tenantRows =
        statsDailyRepo.findByDimensionTypeAndDimensionIdAndRefDateBetween("tenant", null, from, to);

    Map<UUID, AggregatedTenant> tenantAgg = new HashMap<>();
    for (StatsDailyEntity row : tenantRows) {
      UUID tenantId = row.getDimensionId();
      if (tenantId == null) continue;
      AggregatedTenant agg = tenantAgg.get(tenantId);
      if (agg == null) {
        agg = new AggregatedTenant();
        tenantAgg.put(tenantId, agg);
      }
      agg.ticketsCount += row.getTicketsCount();
      agg.stakeSumCents += row.getStakeSumCents();
      agg.netRevenueCents += row.getNetRevenueCents();
    }

    // resolve tenant names
    Map<UUID, String> tenantNames =
        tenantRepo.findAllById(tenantAgg.keySet()).stream()
            .collect(
                Collectors.toMap(
                    com.tchalanet.server.core.tenant.infra.persistence.TenantJpaEntity::getId,
                    com.tchalanet.server.core.tenant.infra.persistence.TenantJpaEntity::getName));
    // Note: above fully-qualified references are required in this context

    var tenants =
        tenantAgg.entrySet().stream()
            .map(
                e ->
                    new PlatformTenantStatsDto(
                        e.getKey(),
                        tenantNames.getOrDefault(e.getKey(), "Tenant " + e.getKey()),
                        e.getValue().ticketsCount,
                        e.getValue().stakeSumCents,
                        e.getValue().netRevenueCents))
            .sorted(Comparator.comparingLong(PlatformTenantStatsDto::netRevenueCents).reversed())
            .limit(10)
            .toList();

    // 3) top cashiers - aggregate similarly from 'cashier' dimension
    var cashierRows =
        statsDailyRepo.findByDimensionTypeAndDimensionIdAndRefDateBetween(
            "cashier", null, from, to);
    Map<UUID, AggregatedTenant> cashierAgg = new HashMap<>();
    for (StatsDailyEntity row : cashierRows) {
      UUID cashierId = row.getDimensionId();
      if (cashierId == null) continue;
      AggregatedTenant agg = cashierAgg.get(cashierId);
      if (agg == null) {
        agg = new AggregatedTenant();
        cashierAgg.put(cashierId, agg);
      }
      agg.ticketsCount += row.getTicketsCount();
      agg.stakeSumCents += row.getStakeSumCents();
      agg.netRevenueCents += row.getNetRevenueCents();
    }

    var topCashiers =
        cashierAgg.entrySet().stream()
            .map(
                e ->
                    new PlatformCashierStatsDto(
                        e.getKey(),
                        "Cashier " + e.getKey(),
                        e.getValue().ticketsCount,
                        e.getValue().stakeSumCents,
                        e.getValue().netRevenueCents))
            .sorted(Comparator.comparingLong(PlatformCashierStatsDto::netRevenueCents).reversed())
            .limit(10)
            .toList();

    // 4) game breakdown: use stats_draw or tenant-level breakdown; for now aggregate by game_code
    // using stats_draw
    var drawRows =
        statsDrawRepo.findByTenantIdAndScheduledAtBetween(
            null,
            from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            to.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
    Map<String, AggregatedGame> gameAgg = new HashMap<>();
    for (var dr : drawRows) {
      AggregatedGame g = gameAgg.get(dr.getGameCode());
      if (g == null) {
        g = new AggregatedGame();
        gameAgg.put(dr.getGameCode(), g);
      }
      g.ticketsCount += dr.getTicketsCount();
      g.stakeSumCents += dr.getStakeSumCents();
      g.netRevenueCents += dr.getNetRevenueCents();
    }
    List<PlatformGameBreakdownItemDto> gameBreakdown =
        gameAgg.entrySet().stream()
            .map(
                e ->
                    new PlatformGameBreakdownItemDto(
                        e.getKey(),
                        e.getValue().ticketsCount,
                        e.getValue().stakeSumCents,
                        e.getValue().netRevenueCents))
            .sorted(
                Comparator.comparingLong(PlatformGameBreakdownItemDto::netRevenueCents).reversed())
            .toList();

    // 5) summary counts
    long totalTenants = tenantRepo.count();
    long totalOutlets = outletRepo.count();
    long totalCashiers = tenantUserRepo.count();

    PlatformSummaryCardDto summary =
        new PlatformSummaryCardDto(
            totalTenants,
            totalOutlets,
            totalCashiers,
            totalTickets,
            totalStake,
            totalWinnings,
            totalNet);

    return new PlatformDashboardStatsResponse(
        from, to, summary, tenants, dailyPoints, gameBreakdown, topCashiers);
  }

  private static class AggregatedTenant {
    long ticketsCount = 0L;
    long stakeSumCents = 0L;
    long netRevenueCents = 0L;
  }

  private static class AggregatedGame {
    long ticketsCount = 0L;
    long stakeSumCents = 0L;
    long netRevenueCents = 0L;
  }
}
