package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitScopeRef;

public record GetExposureAlertsOverviewQuery(
    TenantId tenantId,
    DrawId drawId,
    LimitScopeRef scope,
    int limit
) {}
