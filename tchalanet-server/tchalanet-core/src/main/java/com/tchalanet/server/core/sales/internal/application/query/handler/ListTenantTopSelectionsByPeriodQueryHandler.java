package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.query.ListTenantTopSelectionsByPeriodQuery;
import com.tchalanet.server.core.sales.api.query.TenantTopSelectionsByPeriodView;
import com.tchalanet.server.core.sales.internal.application.port.out.TenantTopSelectionsReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTenantTopSelectionsByPeriodQueryHandler
    implements QueryHandler<ListTenantTopSelectionsByPeriodQuery, TenantTopSelectionsByPeriodView> {

  private final TenantTopSelectionsReaderPort reader;

  @Override
  public TenantTopSelectionsByPeriodView handle(ListTenantTopSelectionsByPeriodQuery query) {
    int limit = Math.min(Math.max(query.limit(), 1), 20);
    return reader.findTopSelectionsByPeriod(
        query.tenantId(),
        query.from(),
        query.to(),
        query.zoneId(),
        limit);
  }
}
