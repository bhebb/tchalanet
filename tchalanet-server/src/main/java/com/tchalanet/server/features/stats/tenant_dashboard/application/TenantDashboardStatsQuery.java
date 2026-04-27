package com.tchalanet.server.features.stats.tenant_dashboard.application;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record TenantDashboardStatsQuery(TenantId tenantId, LocalDate fromDate, LocalDate toDate) {}
