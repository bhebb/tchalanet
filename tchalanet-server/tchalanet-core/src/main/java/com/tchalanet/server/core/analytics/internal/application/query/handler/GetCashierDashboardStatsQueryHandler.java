package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.CashierDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.CashierDashboardStatsView.CashierSummaryCard;
import com.tchalanet.server.core.analytics.api.query.GetCashierDashboardStatsQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles {@link GetCashierDashboardStatsQuery}.
 *
 * <p>Reads the SELLER-dimension row for today to provide KPIs to the cashier dashboard.
 * Returns zeroes if no projection row exists yet (first sale not yet processed).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetCashierDashboardStatsQueryHandler
    implements QueryHandler<GetCashierDashboardStatsQuery, CashierDashboardStatsView> {

  private final AnalyticsDailyRepository repo;

  @Override
  public CashierDashboardStatsView handle(GetCashierDashboardStatsQuery query) {
    Optional<AnalyticsDailyEntity> row =
        repo.findSellerRow(query.tenantId().value(), query.sellerId().value(), query.refDate());

    CashierSummaryCard today = row
        .map(r -> new CashierSummaryCard(
            r.getTicketsSoldCount(),
            fromCents(r.getGrossSalesCents()),
            fromCents(r.getWinningsCalculatedCents()),
            fromCents(r.getGrossSalesCents() - r.getWinningsCalculatedCents())))
        .orElse(new CashierSummaryCard(0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

    return new CashierDashboardStatsView(query.refDate(), today, List.of());
  }

  private static BigDecimal fromCents(long cents) {
    return BigDecimal.valueOf(cents).movePointLeft(2);
  }
}
