package com.tchalanet.server.platform.entitlement.internal;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.model.TenantCapabilitySnapshot;

public interface EntitlementCapabilitiesGetter {
    TenantCapabilitySnapshot getSnapshot(TenantId tenantId);
}
