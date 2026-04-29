package com.tchalanet.server.features.stats.tenantdashboard.app;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantDashboardStatsCriteria;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantDashboardStatsResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantDashboardStatsService {

  private final TenantDashboardFromAggregatesService aggregatesService;

  public TenantDashboardStatsResponse getStats(
      TenantId tenantId, LocalDate fromDate, LocalDate toDate) {
    var criteria = new TenantDashboardStatsCriteria(tenantId, fromDate, toDate);
    return aggregatesService.getStats(criteria);
  }
}
