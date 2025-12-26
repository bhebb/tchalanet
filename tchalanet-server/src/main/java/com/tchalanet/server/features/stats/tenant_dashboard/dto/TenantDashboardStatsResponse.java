package com.tchalanet.server.features.stats.tenant_dashboard.dto;

public record TenantDashboardStatsResponse(TenantDashboardStatsDto stats) {
  public static TenantDashboardStatsResponse empty() {
    return new TenantDashboardStatsResponse(
        new TenantDashboardStatsDto(null, null, null, null, null));
  }
}
