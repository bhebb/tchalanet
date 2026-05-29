package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView.TenantDailyPoint;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView.TenantSummaryCard;
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
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

  @Override
  public TenantDashboardStatsView handle(GetTenantDashboardStatsQuery query) {
    UUID tenantId = query.tenantId().value();
    List<AnalyticsDailyEntity> rows =
        repo.findTenantRows(tenantId, query.from(), query.to());

    long   totalTickets  = 0L;
    long   grossCents    = 0L;
    long   winningsCents = 0L;
    long   payoutsCents  = 0L;
    long   sessions      = 0L;

    for (AnalyticsDailyEntity r : rows) {
      totalTickets  += r.getTicketsSoldCount();
      grossCents    += r.getGrossSalesCents();
      winningsCents += r.getWinningsCalculatedCents();
      payoutsCents  += r.getPayoutsPaidCents();
      sessions      += r.getSessionsOpenedCount();
    }

    TenantSummaryCard summary = new TenantSummaryCard(
        totalTickets,
        fromCents(grossCents),
        fromCents(winningsCents),
        fromCents(payoutsCents),
        fromCents(grossCents - winningsCents),
        sessions);

    List<TenantDailyPoint> daily = rows.stream()
        .map(r -> new TenantDailyPoint(r.getRefDate(),
            r.getTicketsSoldCount(), fromCents(r.getGrossSalesCents())))
        .toList();

    return new TenantDashboardStatsView(query.from(), query.to(), summary, daily, List.of());
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }
}
