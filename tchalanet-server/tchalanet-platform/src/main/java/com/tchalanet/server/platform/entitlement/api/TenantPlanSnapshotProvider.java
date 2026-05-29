package com.tchalanet.server.platform.entitlement.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.model.TenantPlanSnapshot;
import java.util.Optional;

public interface TenantPlanSnapshotProvider {

    Optional<TenantPlanSnapshot> findCurrentPlan(TenantId tenantId);
}
