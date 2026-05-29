package com.tchalanet.server.core.analytics.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.SalesReportLine;
import java.time.LocalDate;
import java.util.List;

/**
 * Returns a per-day + per-game sales breakdown for a tenant.
 *
 * <p>{@code gameCode} is optional — null means "all games".
 */
public record GetSalesReportQuery(
    TenantId  tenantId,
    LocalDate fromDate,
    LocalDate toDate,
    String    gameCode    // nullable
) implements Query<List<SalesReportLine>> {}
