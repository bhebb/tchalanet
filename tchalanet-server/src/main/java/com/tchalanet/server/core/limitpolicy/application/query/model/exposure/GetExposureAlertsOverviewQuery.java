package com.tchalanet.server.core.limitpolicy.application.query.model.exposure;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;

public record GetExposureAlertsOverviewQuery(
    TenantId tenantId,
    DrawId drawId,
    LimitScopeRef scope,
    int limit
) {}
