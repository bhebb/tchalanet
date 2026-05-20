package com.tchalanet.server.catalog.tenant.internal.cache;

/**
 * Cache names for catalog/tenant.
 * Per catalog conventions: cache names are internal implementation detail.
 */
public final class TenantCacheNames {
    private TenantCacheNames() {}

    public static final String TENANT_BY_ID = "catalog:tenant:tenant_by_id";
    public static final String TENANT_BY_CODE = "catalog:tenant:tenant_by_code";
    public static final String BOOTSTRAP_BY_ID = "catalog:tenant:bootstrap_by_id";
    public static final String BOOTSTRAP_BY_CODE = "catalog:tenant:bootstrap_by_code";
    public static final String ACTIVE_TENANT_IDS = "catalog:tenant:active_tenant_ids";
}
