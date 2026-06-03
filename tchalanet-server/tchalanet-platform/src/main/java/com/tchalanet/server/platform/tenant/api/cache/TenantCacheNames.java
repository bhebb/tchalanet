package com.tchalanet.server.platform.tenant.api.cache;

/**
 * Cache names for platform.tenant registry reads.
 * Exposed in api/cache so TenantPersistenceAdapter (write side) can evict on mutations.
 */
public final class TenantCacheNames {
    private TenantCacheNames() {}

    public static final String REGISTRY_BY_CODE  = "platform.tenant.cache.REGISTRY_BY_CODE";
    public static final String REGISTRY_BY_ID    = "platform.tenant.cache.REGISTRY_BY_ID";
    public static final String ACTIVE_TENANT_IDS = "platform.tenant.cache.ACTIVE_TENANT_IDS";
}
