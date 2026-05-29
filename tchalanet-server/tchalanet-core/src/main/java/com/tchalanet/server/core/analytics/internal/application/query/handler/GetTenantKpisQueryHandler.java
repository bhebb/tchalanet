package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import com.tchalanet.server.core.analytics.api.query.GetTenantKpisQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.TenantKpisAnalyticsReader;
import lombok.RequiredArgsConstructor;

/**
 * Handles {@link GetTenantKpisQuery} — reads aggregated tenant KPIs from analytics_daily.
 */
@UseCase
@RequiredArgsConstructor
public class GetTenantKpisQueryHandler
    implements QueryHandler<GetTenantKpisQuery, TenantKpisView> {

  private final TenantKpisAnalyticsReader reader;

  @Override
  public TenantKpisView handle(GetTenantKpisQuery query) {
    if (query.tenantId() == null) {
      return TenantKpisView.empty();
    }
    return reader.computeTenantKpis(
        query.tenantId().value(), query.fromDate(), query.toDate());
  }
}
