package com.tchalanet.server.features.stats.tenantdashboard.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record TenantDashboardStatsCriteria(TenantId tenantId, LocalDate fromDate, LocalDate toDate) {}
