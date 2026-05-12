package com.tchalanet.server.features.stats.tenantdashboard.model;

public record TenantDashboardStatsResponse(TenantDashboardStatsView stats) {
  public static TenantDashboardStatsResponse empty() {
    return new TenantDashboardStatsResponse(
        new TenantDashboardStatsView(null, null, null, null, null));
  }
}
