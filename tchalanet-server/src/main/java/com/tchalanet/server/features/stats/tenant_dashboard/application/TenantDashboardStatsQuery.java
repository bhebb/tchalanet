package com.tchalanet.server.features.stats.tenant_dashboard.application;

import java.time.LocalDate;
import java.util.UUID;

public record TenantDashboardStatsQuery(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
}

