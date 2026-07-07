package com.tchalanet.server.platform.tenant.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import com.tchalanet.server.platform.tenant.api.cache.TenantCacheNames;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Tenant registry lookups. Change rarely and are evicted on write, but carry status
 * (ACTIVE/SUSPENDED) which gates access — kept at Tier B (15 min / 6 h) rather than the longer
 * referential tier so a status change propagates reasonably even absent an eviction.
 */
@Component
public class TenantCacheSpecProvider implements CacheSpecProvider {

  private static final Duration L1 = Duration.ofMinutes(15);
  private static final Duration L2 = Duration.ofHours(6);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(TenantCacheNames.REGISTRY_BY_CODE, L1, L2),
        CacheSpec.of(TenantCacheNames.REGISTRY_BY_ID, L1, L2),
        CacheSpec.of(TenantCacheNames.ACTIVE_TENANT_IDS, L1, L2));
  }
}
