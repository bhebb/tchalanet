package com.tchalanet.server.core.offlinesync.api.query.dashboard;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;

public record GetOfflineDashboardQuery(TenantId tenantId)
    implements Query<OfflineDashboardView> {
}
