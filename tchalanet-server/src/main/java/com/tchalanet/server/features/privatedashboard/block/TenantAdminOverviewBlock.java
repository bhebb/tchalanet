// java
package com.tchalanet.server.features.privatedashboard.block;

import com.tchalanet.server.features.reporting.outletperformance.OutletPerformanceReportResponse;
import com.tchalanet.server.features.stats.cashier_dashboard.dto.CashierDashboardStatsResponse;
import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantDashboardStatsResponse;
import java.util.List;

public record TenantAdminOverviewBlock(
    TenantDashboardStatsResponse tenantStats,
    OutletPerformanceReportResponse outletPerformance,
    List<CashierDashboardStatsResponse> cashierStats) {
  public static TenantAdminOverviewBlock empty() {
    return new TenantAdminOverviewBlock(null, null, List.of());
  }
}
