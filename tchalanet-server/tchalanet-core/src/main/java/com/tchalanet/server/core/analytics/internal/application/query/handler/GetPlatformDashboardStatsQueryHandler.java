package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView.PlatformDailyPoint;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView.PlatformGameBreakdown;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView.PlatformSummaryCard;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView.TenantRankRow;
import com.tchalanet.server.core.analytics.api.query.GetPlatformDashboardStatsQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsGameBreakdownReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles {@link GetPlatformDashboardStatsQuery}.
 *
 * <p>Reads PLATFORM rows for the global summary and TENANT rows for the
 * per-tenant ranking (top N by gross sales).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetPlatformDashboardStatsQueryHandler
    implements QueryHandler<GetPlatformDashboardStatsQuery, PlatformDashboardStatsView> {

  private final AnalyticsDailyRepository repo;
  private final AnalyticsGameBreakdownReader gameBreakdownReader;

  @Override
  public PlatformDashboardStatsView handle(GetPlatformDashboardStatsQuery query) {
    // Global rollup from PLATFORM rows
    List<AnalyticsDailyEntity> platformRows =
        repo.findPlatformRows(query.from(), query.to());

    long ticketsSold    = 0L;
    long grossCents     = 0L;
    long winningsCents  = 0L;
    long payoutsCents   = 0L;
    long commissionCents = 0L;
    long tenantChargeCents = 0L;
    long promotionLines = 0L;
    long promotionPotentialPayoutCents = 0L;

    for (AnalyticsDailyEntity r : platformRows) {
      ticketsSold   += r.getTicketsSoldCount();
      grossCents    += r.getGrossSalesCents();
      winningsCents += r.getWinningsCalculatedCents();
      payoutsCents  += r.getPayoutsPaidCents();
      commissionCents += r.getSellerCommissionCents();
      tenantChargeCents += r.getTenantChargeCents();
      promotionLines += r.getPromotionLineCount();
      promotionPotentialPayoutCents += r.getPromotionPotentialPayoutCents();
    }

    List<PlatformDailyPoint> daily = platformRows.stream()
        .map(r -> new PlatformDailyPoint(
            r.getRefDate(),
            r.getTicketsSoldCount(),
            fromCents(r.getGrossSalesCents()),
            fromCents(r.getWinningsCalculatedCents()),
            fromCents(r.getPayoutsPaidCents()),
            fromCents(r.getSellerCommissionCents()),
            fromCents(r.getTenantChargeCents()),
            r.getPromotionLineCount(),
            fromCents(r.getPromotionPotentialPayoutCents()),
            fromCents(r.getNetRevenueEstimatedCents()),
            fromCents(r.getNetRevenuePaidBasisCents())))
        .toList();

    // Per-tenant rows for ranking
    List<AnalyticsDailyEntity> tenantRows =
        repo.findAllTenantRows(query.from(), query.to());

    // Aggregate per tenantId
    Map<UUID, long[]> tenantAgg = new HashMap<>();
    for (AnalyticsDailyEntity r : tenantRows) {
      tenantAgg.computeIfAbsent(r.getTenantId(), k -> new long[5]);
      long[] acc = tenantAgg.get(r.getTenantId());
      acc[0] += r.getTicketsSoldCount();
      acc[1] += r.getGrossSalesCents();
      acc[2] += r.getWinningsCalculatedCents();
      acc[3] += r.getSellerCommissionCents();
      acc[4] += r.getTenantChargeCents();
    }

    List<TenantRankRow> topTenants = new ArrayList<>();
    tenantAgg.entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1])) // desc gross sales
        .limit(query.topTenantsLimit())
        .forEach(e -> topTenants.add(new TenantRankRow(
            e.getKey().toString(), // tenant code not available here — consumer can enrich
            e.getValue()[0],
            fromCents(e.getValue()[1]),
            fromCents(e.getValue()[1] - e.getValue()[2] - e.getValue()[3] - e.getValue()[4]))));

    PlatformSummaryCard summary = new PlatformSummaryCard(
        tenantAgg.size(),
        ticketsSold,
        fromCents(grossCents),
        fromCents(winningsCents),
        fromCents(payoutsCents),
        fromCents(commissionCents),
        fromCents(tenantChargeCents),
        promotionLines,
        fromCents(promotionPotentialPayoutCents),
        fromCents(grossCents - winningsCents - commissionCents - tenantChargeCents),
        fromCents(grossCents - payoutsCents - commissionCents - tenantChargeCents));

    List<PlatformGameBreakdown> gameBreakdown = gameBreakdownReader
        .findPlatformGameBreakdown(query.from(), query.to(), query.gameBreakdownLimit())
        .stream()
        .map(row -> new PlatformGameBreakdown(
            row.gameCode(),
            row.gameCode(),
            row.ticketsSold(),
            row.grossSales(),
            row.netRevenueEstimated()))
        .toList();

    return new PlatformDashboardStatsView(
        query.from(), query.to(), summary, daily, gameBreakdown, topTenants);
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }
}
