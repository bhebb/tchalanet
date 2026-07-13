package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.time.ZoneId;

public record ListTenantTopSelectionsByPeriodQuery(
    TenantId tenantId,
    LocalDate from,
    LocalDate to,
    ZoneId zoneId,
    int limit
) implements Query<TenantTopSelectionsByPeriodView> {}
