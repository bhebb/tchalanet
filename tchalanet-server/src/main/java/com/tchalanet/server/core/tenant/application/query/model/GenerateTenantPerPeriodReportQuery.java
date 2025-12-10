package com.tchalanet.server.core.tenant.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.time.YearMonth;
import java.util.UUID;

public record GenerateTenantPerPeriodReportQuery(UUID tenantId, YearMonth month) implements Query<String> {}
