package com.tchalanet.server.platform.entitlement.internal;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.entitlement.api.EntitlementApi;
import com.tchalanet.server.platform.entitlement.api.model.TenantCapabilitySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.OptionalInt;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementService implements EntitlementApi {

    private final EntitlementCapabilitiesGetter entitlementCapabilitiesGetter;

    @Override
    public TenantCapabilitySnapshot getSnapshot(TenantId tenantId) {
        return entitlementCapabilitiesGetter.getSnapshot(tenantId);
    }

    @Override
    public boolean checkFeature(TenantId tenantId, String featureKey) {
        return entitlementCapabilitiesGetter.getSnapshot(tenantId).hasFeature(featureKey);
    }

    @Override
    public void requireFeature(TenantId tenantId, String featureKey) {
        if (!checkFeature(tenantId, featureKey)) {
            throw ProblemRest.forbidden("entitlement.feature_required", Map.of("feature", featureKey));
        }
    }

    @Override
    public OptionalInt limitValue(TenantId tenantId, String limitKey) {
        return entitlementCapabilitiesGetter.getSnapshot(tenantId).getLimit(limitKey);
    }

    @Override
    public void requireLimitAtMost(TenantId tenantId, String limitKey, int currentUsage) {
        int limit = limitValue(tenantId, limitKey)
            .orElseThrow(() -> ProblemRest.internal("Missing entitlement limit: " + limitKey));
        if (currentUsage > limit) {
            throw ProblemRest.forbidden("entitlement.limit_exceeded",
                Map.of("limitKey", limitKey, "limit", limit, "current", currentUsage));
        }
    }
}
