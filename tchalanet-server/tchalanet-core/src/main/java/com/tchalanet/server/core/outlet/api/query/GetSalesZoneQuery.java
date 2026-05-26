package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetSalesZoneQuery(TenantId tenantId, SalesZoneId zoneId)
    implements Query<SalesZoneView> {}
