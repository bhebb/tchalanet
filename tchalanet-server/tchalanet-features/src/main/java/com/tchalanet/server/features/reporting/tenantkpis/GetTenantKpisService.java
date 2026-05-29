package com.tchalanet.server.features.reporting.tenantkpis;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import com.tchalanet.server.core.analytics.api.query.GetTenantKpisQuery;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Tenant KPIs service — delegates to core.analytics via QueryBus. */
@Service
@RequiredArgsConstructor
public class GetTenantKpisService {

  private final QueryBus queryBus;

  public KpisResponse getKpis(TenantKpisCriteria criteria) {
    TenantId tenantId = criteria.tenantId() != null ? TenantId.of(criteria.tenantId()) : null;
    TenantKpisView view = queryBus.ask(
        new GetTenantKpisQuery(tenantId, criteria.fromDate(), criteria.toDate()));

    KpisView kpisView = toKpisView(view);
    var snapshot = new GetTenantKpisSnapshot(criteria.fromDate(), criteria.toDate(), kpisView);
    return new KpisResponse(snapshot);
  }

  private KpisView toKpisView(TenantKpisView view) {
    if (view == null) {
      return new KpisView(0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L,
          null, null, 0, null);
    }
    BigDecimal payoutRatio = view.totalSales().compareTo(BigDecimal.ZERO) > 0
        ? view.totalPayout().divide(view.totalSales(), 4, java.math.RoundingMode.HALF_UP)
        : null;
    BigDecimal avgTicket = view.ticketsSold() > 0
        ? view.totalSales().divide(BigDecimal.valueOf(view.ticketsSold()), 2,
            java.math.RoundingMode.HALF_UP)
        : null;

    return new KpisView(
        view.ticketsSold(),
        view.totalSales(),
        view.totalPayout(),
        view.netRevenue(),
        view.activeOutlets(),
        view.activeCashiers(),
        payoutRatio,
        avgTicket,
        0,
        null);
  }
}
