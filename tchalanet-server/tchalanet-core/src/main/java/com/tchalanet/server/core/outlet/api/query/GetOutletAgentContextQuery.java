package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetOutletAgentContextQuery(
    TenantId tenantId,
    OutletId outletId
) implements Query<OutletAgentContextView> {}
