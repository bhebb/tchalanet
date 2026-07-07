package com.tchalanet.server.platform.tenanttheme.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/** Tenant theme runtime: read on every public page, changed rarely, evicted on write. Tier B. */
@Component
public class TenantThemeCacheSpecProvider implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(TenantThemeCacheNames.RUNTIME, Duration.ofMinutes(15), Duration.ofHours(6)));
  }
}
