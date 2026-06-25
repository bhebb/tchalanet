package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView.TenantDailyPoint;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView.TenantSummaryCard;
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsGameBreakdownReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles {@link GetTenantDashboardStatsQuery}.
 *
 * <p>Reads {@code TENANT} dimension rows from {@code analytics_daily} for the
 * requested window and builds the view. Game breakdown requires a separate
 * GAME-dimension query (V2 — currently returns empty list).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetTenantDashboardStatsQueryHandler
    implements QueryHandler<GetTenantDashboardStatsQuery, TenantDashboardStatsView> {

  private final AnalyticsDailyRepository repo;
  private final AnalyticsGameBreakdownReader gameBreakdownReader;

  @Override
  public TenantDashboardStatsView handle(GetTenantDashboardStatsQuery query) {
    UUID tenantId = query.tenantId().value();
    List<AnalyticsDailyEntity> rows =
        repo.findTenantRows(tenantId, query.from(), query.to());

    long   totalTickets  = 0L;
    long   grossCents    = 0L;
    long   winningsCents = 0L;
    long   payoutsCents  = 0L;
    long   commissionCents = 0L;
    long   buyerChargeCents = 0L;
    long   sellerChargeCents = 0L;
    long   tenantChargeCents = 0L;
    long   waivedChargeCents = 0L;
    long   promotionLines = 0L;
    long   promotionPricedLines = 0L;
    long   promotionPayoutBaseCents = 0L;
    long   promotionPotentialPayoutCents = 0L;
    long   sessions      = 0L;

    for (AnalyticsDailyEntity r : rows) {
      totalTickets  += r.getTicketsSoldCount();
      grossCents    += r.getGrossSalesCents();
      winningsCents += r.getWinningsCalculatedCents();
      payoutsCents  += r.getPayoutsPaidCents();
      commissionCents += r.getSellerCommissionCents();
      buyerChargeCents += r.getBuyerChargeCents();
      sellerChargeCents += r.getSellerChargeCents();
      tenantChargeCents += r.getTenantChargeCents();
      waivedChargeCents += r.getWaivedChargeCents();
      promotionLines += r.getPromotionLineCount();
      promotionPricedLines += r.getPromotionPricedLineCount();
      promotionPayoutBaseCents += r.getPromotionPayoutBaseCents();
      promotionPotentialPayoutCents += r.getPromotionPotentialPayoutCents();
      sessions      += r.getSessionsOpenedCount();
    }

    TenantSummaryCard summary = new TenantSummaryCard(
        totalTickets,
        fromCents(grossCents),
        fromCents(winningsCents),
        fromCents(payoutsCents),
        fromCents(commissionCents),
        fromCents(buyerChargeCents),
        fromCents(sellerChargeCents),
        fromCents(tenantChargeCents),
        fromCents(waivedChargeCents),
        promotionLines,
        promotionPricedLines,
        fromCents(promotionPayoutBaseCents),
        fromCents(promotionPotentialPayoutCents),
        fromCents(grossCents - winningsCents - commissionCents - tenantChargeCents),
        sessions);

    List<TenantDailyPoint> daily = rows.stream()
        .map(r -> new TenantDailyPoint(r.getRefDate(),
            r.getTicketsSoldCount(),
            fromCents(r.getGrossSalesCents()),
            fromCents(r.getSellerCommissionCents()),
            fromCents(r.getTenantChargeCents()),
            r.getPromotionLineCount(),
            fromCents(r.getPromotionPotentialPayoutCents()),
            fromCents(r.getNetRevenueEstimatedCents())))
        .toList();

    List<TenantDashboardStatsView.TenantGameBreakdown> gameBreakdown = gameBreakdownReader
        .findTenantGameBreakdown(tenantId, query.from(), query.to(), query.topGamesLimit())
        .stream()
        .map(row -> new TenantDashboardStatsView.TenantGameBreakdown(
            row.gameCode(),
            row.gameCode(),
            row.ticketsSold(),
            row.grossSales(),
            row.netRevenueEstimated()))
        .toList();

    return new TenantDashboardStatsView(query.from(), query.to(), summary, daily, gameBreakdown);
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }
}
