package com.tchalanet.server.core.offlinesync.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;

public record GetOfflineRiskDashboardQuery(TenantId tenantId)
    implements Query<OfflineRiskDashboardView> {}

