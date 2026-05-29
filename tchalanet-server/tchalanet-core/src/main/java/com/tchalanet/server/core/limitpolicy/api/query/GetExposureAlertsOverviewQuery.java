package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;

public record GetExposureAlertsOverviewQuery(
    TenantId tenantId,
    DrawId drawId,
    LimitScopeRef scope,
    int limit
) implements Query<ExposureAlertsOverviewView> {}
