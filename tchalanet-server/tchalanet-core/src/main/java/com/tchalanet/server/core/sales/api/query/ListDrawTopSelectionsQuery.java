package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

public record ListDrawTopSelectionsQuery(
    TenantId tenantId,
    DrawId drawId,
    int limit
) implements Query<DrawTopSelectionsView> {}
