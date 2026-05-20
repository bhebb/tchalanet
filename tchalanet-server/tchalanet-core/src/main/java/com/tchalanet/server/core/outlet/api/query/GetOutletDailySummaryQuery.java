package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

/** Query to get daily summary for an outlet */
public record GetOutletDailySummaryQuery(TenantId tenantId, OutletId outletId, LocalDate date)
    implements Query<OutletDailySummary> {}
