package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.List;

public record ListSalesZonesQuery(TenantId tenantId)
    implements Query<List<SalesZoneView>> {}
