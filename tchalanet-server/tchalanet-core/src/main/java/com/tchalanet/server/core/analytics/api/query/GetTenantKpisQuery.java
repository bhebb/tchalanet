package com.tchalanet.server.core.analytics.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import java.time.LocalDate;

/**
 * Returns aggregated KPI metrics for a tenant over the specified date window.
 *
 * <p>Sourced from the {@code analytics_daily} projection table.
 */
public record GetTenantKpisQuery(
    TenantId  tenantId,
    LocalDate fromDate,
    LocalDate toDate
) implements Query<TenantKpisView> {}
