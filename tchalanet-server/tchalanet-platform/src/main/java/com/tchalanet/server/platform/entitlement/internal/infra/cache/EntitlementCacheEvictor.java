package com.tchalanet.server.platform.entitlement.internal.infra.cache;

import static com.tchalanet.server.platform.entitlement.internal.infra.cache.EntitlementCacheSpecProvider.TENANT_SNAPSHOT_CACHE;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.EntitlementCacheInvalidationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitlementCacheEvictor implements EntitlementCacheInvalidationApi {

  private final CacheManager cacheManager;

  @Override
  public void evictTenantSnapshot(TenantId tenantId) {
    Cache cache = cacheManager.getCache(TENANT_SNAPSHOT_CACHE);
    if (cache == null) {
      log.warn("Cache '{}' not found for eviction.", TENANT_SNAPSHOT_CACHE);
      return;
    }
    cache.evict(tenantId.value());
    log.debug("Evicted entitlement tenant snapshot cache for tenantId={}", tenantId);
  }
}
