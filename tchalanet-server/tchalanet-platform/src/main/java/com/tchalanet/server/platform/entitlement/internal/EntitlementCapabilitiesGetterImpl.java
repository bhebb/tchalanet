package com.tchalanet.server.platform.entitlement.internal;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.TenantPlanSnapshotProvider;
import com.tchalanet.server.platform.entitlement.api.model.TenantCapabilitySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementCapabilitiesGetterImpl implements EntitlementCapabilitiesGetter {

    private final PlanCatalog planCatalog;
    private final TenantPlanSnapshotProvider tenantPlanSnapshotProvider;
    private final JsonUtils jsonUtils;
    private final Clock clock;

    @Override
    @Cacheable(cacheNames = "platform.entitlement.tenant_snapshot", key = "#tenantId")
    public TenantCapabilitySnapshot getSnapshot(TenantId tenantId) {
        log.debug("Resolving entitlement snapshot for tenant {}", tenantId);

        // 1. Resolve subscription via TenantPlanSnapshotProvider (cross-domain port)
        var tenantPlanSnapshot = tenantPlanSnapshotProvider.findCurrentPlan(tenantId);

        if (tenantPlanSnapshot.isEmpty() || !tenantPlanSnapshot.get().activeAt(Instant.now(clock))) {
            return new TenantCapabilitySnapshot(tenantId, "NONE", false, Collections.emptyMap(), Collections.emptyMap(), Instant.now(clock));
        }

        var sub = tenantPlanSnapshot.get();

        // 2. Resolve plan via catalog.plan
        var plan = planCatalog.findByCode(sub.planCode())
            .orElseThrow(() -> new IllegalStateException("Plan not found: " + sub.planCode()));

        // 3. Parse JSON capabilities
        Map<String, Boolean> features = parseFeatures(plan);
        Map<String, Integer> limits = parseLimits(plan);

        return new TenantCapabilitySnapshot(
            tenantId,
            plan.code(),
            true,
            features,
            limits,
            Instant.now(clock)
        );
    }

    private Map<String, Boolean> parseFeatures(PlanView plan) {
        if (plan.featuresJson() == null || plan.featuresJson().isNull()) return Collections.emptyMap();
        try {
            // Using Jackson's convertValue from JsonNode to Map
            return jsonUtils.convertValue(plan.featuresJson(), new TypeReference<Map<String, Boolean>>() {
            });
        } catch (Exception e) {
            log.error("Error parsing featuresJson for plan {}", plan.code(), e);
            return Collections.emptyMap();
        }
    }

    private Map<String, Integer> parseLimits(PlanView plan) {
        if (plan.limitsJson() == null || plan.limitsJson().isNull()) return Collections.emptyMap();
        try {
            return jsonUtils.convertValue(plan.limitsJson(), new TypeReference<Map<String, Integer>>() {
            });
        } catch (Exception e) {
            log.error("Error parsing limitsJson for plan {}", plan.code(), e);
            return Collections.emptyMap();
        }
    }
}
