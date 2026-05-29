package com.tchalanet.server.platform.entitlement.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.model.TenantCapabilitySnapshot;

import java.util.OptionalInt;

/**
 * Public API for checking tenant entitlements (features and limits).
 */
public interface EntitlementApi {

    /**
     * Resolves the full capability snapshot for a tenant.
     */
    TenantCapabilitySnapshot getSnapshot(TenantId tenantId);

    /**
     * Checks if a feature is enabled.
     */
    boolean checkFeature(TenantId tenantId, String featureKey);

    /**
     * Requires a feature to be enabled, otherwise throws ProblemRest.forbidden.
     */
    void requireFeature(TenantId tenantId, String featureKey);

    /**
     * Gets the numeric limit for a key, if the tenant plan defines one.
     */
    OptionalInt limitValue(TenantId tenantId, String limitKey);

    /**
     * Ensures current usage is within limit.
     */
    void requireLimitAtMost(TenantId tenantId, String limitKey, int currentUsage);
}
