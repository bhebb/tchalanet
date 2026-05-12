package com.tchalanet.server.core.offlinesync.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;

public record GetOfflineRiskDashboardQuery(TenantId tenantId)
    implements Query<OfflineRiskDashboardView> {}

