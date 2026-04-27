package com.tchalanet.server.catalog.tenant.api.cache;

/**
 * Cache names for catalog/tenant.
 *
 * EXCEPTIONAL: Normally cache names are internal implementation details
 * and should live in catalog/tenant/internal/cache.
 *
 * However, they are exposed here in api/cache to allow core/tenantconfig
 * to evict these caches when tenant data is modified (create/update operations).
 *
 * This is an architectural exception:
 * - core/tenantconfig writes to tenant table
 * - catalog/tenant reads from tenant table (with caching)
 * - core/tenantconfig needs to evict catalog/tenant caches on write
 * - Therefore, cache names must be in public API
 *
 * Per user request: "normalement les caches sont dns internal et devrait pas
 * etre accessible a l'exterieur. exceptionnellement il faut les sortir"
 */
public final class TenantCacheNames {
    private TenantCacheNames() {}

    public static final String TENANT_BY_ID = "catalog.tenant.cache.TENANT_BY_ID";
    public static final String TENANT_BY_CODE = "catalog.tenant.cache.TENANT_BY_CODE";
    public static final String BOOTSTRAP_BY_ID = "catalog.tenant.cache.BOOTSTRAP_BY_ID";
    public static final String BOOTSTRAP_BY_CODE = "catalog.tenant.cache.BOOTSTRAP_BY_CODE";
    public static final String ACTIVE_TENANT_IDS = "catalog.tenant.cache.ACTIVE_TENANT_IDS";
}
