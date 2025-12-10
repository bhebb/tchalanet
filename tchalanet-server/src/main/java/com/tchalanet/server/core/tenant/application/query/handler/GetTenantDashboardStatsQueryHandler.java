package com.tchalanet.server.core.tenant.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.query.model.GetTenantDashboardStatsQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetTenantDashboardStatsQueryHandler implements QueryHandler<GetTenantDashboardStatsQuery, Map<String,Object>> {

  @Override
  public Map<String,Object> handle(GetTenantDashboardStatsQuery query) {
    // TODO: compute and return metrics for dashboard
    throw new UnsupportedOperationException("GetTenantDashboardStatsQueryHandler not implemented yet");
  }
}

