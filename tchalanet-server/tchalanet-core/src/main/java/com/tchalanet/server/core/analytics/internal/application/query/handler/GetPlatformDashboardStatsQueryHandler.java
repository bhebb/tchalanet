package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView.PlatformSummaryCard;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView.TenantRankRow;
import com.tchalanet.server.core.analytics.api.query.GetPlatformDashboardStatsQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
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

  @Override
  public PlatformDashboardStatsView handle(GetPlatformDashboardStatsQuery query) {
    // Global rollup from PLATFORM rows
    List<AnalyticsDailyEntity> platformRows =
        repo.findPlatformRows(query.from(), query.to());

    long ticketsSold    = 0L;
    long grossCents     = 0L;
    long winningsCents  = 0L;

    for (AnalyticsDailyEntity r : platformRows) {
      ticketsSold   += r.getTicketsSoldCount();
      grossCents    += r.getGrossSalesCents();
      winningsCents += r.getWinningsCalculatedCents();
    }

    // Per-tenant rows for ranking
    List<AnalyticsDailyEntity> tenantRows =
        repo.findAllTenantRows(query.from(), query.to());

    // Aggregate per tenantId
    Map<UUID, long[]> tenantAgg = new HashMap<>();
    for (AnalyticsDailyEntity r : tenantRows) {
      tenantAgg.computeIfAbsent(r.getTenantId(), k -> new long[3]);
      long[] acc = tenantAgg.get(r.getTenantId());
      acc[0] += r.getTicketsSoldCount();
      acc[1] += r.getGrossSalesCents();
      acc[2] += r.getWinningsCalculatedCents();
    }

    List<TenantRankRow> topTenants = new ArrayList<>();
    tenantAgg.entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1])) // desc gross sales
        .limit(query.topTenantsLimit())
        .forEach(e -> topTenants.add(new TenantRankRow(
            e.getKey().toString(), // tenant code not available here — consumer can enrich
            e.getValue()[0],
            fromCents(e.getValue()[1]),
            fromCents(e.getValue()[1] - e.getValue()[2]))));

    PlatformSummaryCard summary = new PlatformSummaryCard(
        tenantAgg.size(),
        ticketsSold,
        fromCents(grossCents),
        fromCents(winningsCents),
        fromCents(grossCents - winningsCents));

    return new PlatformDashboardStatsView(query.from(), query.to(), summary, topTenants);
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }
}
