package com.tchalanet.server.features.stats.tenant_dashboard.application;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyJpaRepository;
import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantDailySalesPointDto;
import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantDashboardStatsDto;
import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantDashboardStatsResponse;
import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantGameBreakdownItemDto;
import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantSummaryCardDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TenantDashboardFromAggregatesService {

  private final StatsDailyJpaRepository statsDailyRepo;

  @Transactional(readOnly = true)
  public TenantDashboardStatsResponse compute(TenantDashboardStatsQuery query) {
    TenantId tenantId = query.tenantId();
    LocalDate from = query.fromDate();
    LocalDate to = query.toDate();

    List<StatsDailyEntity> rows =
        statsDailyRepo.findByDimensionTypeAndDimensionIdAndRefDateBetween(
            "tenant", tenantId.uuid(), from, to);

    long totalTickets = 0;
    long totalStake = 0;
    long totalWinnings = 0;
    long totalNet = 0;

    List<TenantDailySalesPointDto> dailyPoints = new ArrayList<>();

    for (StatsDailyEntity r : rows) {
      totalTickets += r.getTicketsCount();
      totalStake += r.getStakeSumCents();
      totalWinnings += r.getWinningsSumCents();
      totalNet += r.getNetRevenueCents();

      dailyPoints.add(
          new TenantDailySalesPointDto(
              r.getRefDate(),
              BigDecimal.valueOf(r.getStakeSumCents()).movePointLeft(2),
              BigDecimal.valueOf(r.getWinningsSumCents()).movePointLeft(2)));
    }

    TenantSummaryCardDto summary =
        new TenantSummaryCardDto(
            totalTickets,
            BigDecimal.valueOf(totalStake).movePointLeft(2),
            BigDecimal.valueOf(totalWinnings).movePointLeft(2),
            BigDecimal.valueOf(totalNet).movePointLeft(2));

    List<TenantGameBreakdownItemDto> gameBreakdown = List.of();

    TenantDashboardStatsDto dto =
        new TenantDashboardStatsDto(from, to, summary, gameBreakdown, dailyPoints);

    return new TenantDashboardStatsResponse(dto);
  }
}
