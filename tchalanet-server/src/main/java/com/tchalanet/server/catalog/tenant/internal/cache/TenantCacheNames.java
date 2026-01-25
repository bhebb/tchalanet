package com.tchalanet.server.catalog.tenant.internal.cache;

/**
 * Cache names for catalog/tenant.
 * Per catalog conventions: cache names are internal implementation detail.
 */
public final class TenantCacheNames {
    private TenantCacheNames() {}

    public static final String TENANT_BY_ID = "catalog.tenant.cache.TENANT_BY_ID";
    public static final String TENANT_BY_CODE = "catalog.tenant.cache.TENANT_BY_CODE";
    public static final String BOOTSTRAP_BY_ID = "catalog.tenant.cache.BOOTSTRAP_BY_ID";
    public static final String BOOTSTRAP_BY_CODE = "catalog.tenant.cache.BOOTSTRAP_BY_CODE";
    public static final String ACTIVE_TENANT_IDS = "catalog.tenant.cache.ACTIVE_TENANT_IDS";
}
