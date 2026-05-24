package com.tchalanet.server.platform.entitlement.internal.infra.cache;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.EntitlementCacheInvalidationApi; // Import the new API
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import static com.tchalanet.server.platform.entitlement.internal.infra.cache.EntitlementCacheSpecProvider.TENANT_SNAPSHOT_CACHE;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitlementCacheEvictor implements EntitlementCacheInvalidationApi { // Implement the API

    private final CacheManager cacheManager;

    // Removed @EventListener methods as eviction will now be explicitly called

    @Override // Mark as override
    public void evictTenantSnapshot(TenantId tenantId) { // Made public
        Cache cache = cacheManager.getCache(TENANT_SNAPSHOT_CACHE);
        if (cache == null) {
            log.warn("Cache '{}' not found for eviction.", TENANT_SNAPSHOT_CACHE);
            return;
        }
        cache.evict(tenantId.value()); // Changed to evict with UUID value
        log.debug("Evicted entitlement tenant snapshot cache for tenantId={}", tenantId);
    }
}
