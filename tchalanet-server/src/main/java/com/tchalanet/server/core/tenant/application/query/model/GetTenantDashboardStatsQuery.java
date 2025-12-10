package com.tchalanet.server.core.tenant.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;

public record GetTenantDashboardStatsQuery(UUID tenantId, LocalDate since) implements Query<Map<String,Object>> {}
