package com.tchalanet.server.features.stats.tenantdashboard.app;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyJpaRepository;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantDailySalesPoint;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantDashboardStatsResponse;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantDashboardStatsCriteria;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantDashboardStatsView;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantGameBreakdownItem;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantSummaryCard;
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
  public TenantDashboardStatsResponse getStats(TenantDashboardStatsCriteria criteria) {
    TenantId tenantId = criteria.tenantId();
    LocalDate from = criteria.fromDate();
    LocalDate to = criteria.toDate();

    List<StatsDailyEntity> rows =
        statsDailyRepo.findByDimensionTypeAndDimensionIdAndRefDateBetween(
            "tenant", tenantId.uuid(), from, to);

    long totalTickets = 0;
    long totalStake = 0;
    long totalWinnings = 0;
    long totalNet = 0;

    List<TenantDailySalesPoint> dailyPoints = new ArrayList<>();

    for (StatsDailyEntity r : rows) {
      totalTickets += r.getTicketsCount();
      totalStake += r.getStakeSumCents();
      totalWinnings += r.getWinningsSumCents();
      totalNet += r.getNetRevenueCents();

      dailyPoints.add(
          new TenantDailySalesPoint(
              r.getRefDate(),
              BigDecimal.valueOf(r.getStakeSumCents()).movePointLeft(2),
              BigDecimal.valueOf(r.getWinningsSumCents()).movePointLeft(2)));
    }

    TenantSummaryCard summary =
        new TenantSummaryCard(
            totalTickets,
            BigDecimal.valueOf(totalStake).movePointLeft(2),
            BigDecimal.valueOf(totalWinnings).movePointLeft(2),
            BigDecimal.valueOf(totalNet).movePointLeft(2));

    List<TenantGameBreakdownItem> gameBreakdown = List.of();

    TenantDashboardStatsView dto =
        new TenantDashboardStatsView(from, to, summary, gameBreakdown, dailyPoints);

    return new TenantDashboardStatsResponse(dto);
  }
}
