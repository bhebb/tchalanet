package com.tchalanet.server.platform.entitlement.api;

import com.tchalanet.server.common.types.id.TenantId;

public interface EntitlementCacheInvalidationApi {

    void evictTenantSnapshot(TenantId tenantId);
}
