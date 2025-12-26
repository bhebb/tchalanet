package com.tchalanet.server.features.stats.tenant_dashboard.application;

import java.time.LocalDate;
import com.tchalanet.server.common.types.id.TenantId;

public record TenantDashboardStatsQuery(TenantId tenantId, LocalDate fromDate, LocalDate toDate) {
}
