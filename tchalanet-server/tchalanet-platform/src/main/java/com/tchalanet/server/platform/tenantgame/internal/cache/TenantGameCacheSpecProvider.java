package com.tchalanet.server.platform.tenantgame.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Tenant game runtime/projection: read on every POS session, changed rarely, evicted on write. Tier
 * B.
 */
@Component
public class TenantGameCacheSpecProvider implements CacheSpecProvider {

  private static final Duration L1 = Duration.ofMinutes(15);
  private static final Duration L2 = Duration.ofHours(6);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(TenantGameCacheNames.RUNTIME, L1, L2),
        CacheSpec.of(TenantGameCacheNames.PROJECTION, L1, L2));
  }
}
