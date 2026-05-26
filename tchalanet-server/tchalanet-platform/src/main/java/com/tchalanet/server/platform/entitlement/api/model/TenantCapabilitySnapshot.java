package com.tchalanet.server.platform.entitlement.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Immutable snapshot of tenant capabilities (features + limits) derived from subscription and plan.
 */
public record TenantCapabilitySnapshot(
    TenantId tenantId,
    String planCode,
    boolean activeSubscription,
    Map<String, Boolean> features,
    Map<String, Integer> limits,
    Instant resolvedAt
) {
    public boolean hasFeature(String key) {
        return features != null && features.getOrDefault(key, false);
    }

    public OptionalInt getLimit(String key) {
        if (limits == null || !limits.containsKey(key)) {
            return OptionalInt.empty();
        }
        var value = limits.get(key);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }
}
