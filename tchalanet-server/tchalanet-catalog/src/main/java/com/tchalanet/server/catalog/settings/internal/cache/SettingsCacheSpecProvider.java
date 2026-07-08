package com.tchalanet.server.catalog.settings.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/** Resolved settings per tenant, changed rarely and evicted on write. Tier B: 15 min / 6 h. */
@Component
public class SettingsCacheSpecProvider implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(
            SettingsCacheNames.RESOLVED_SETTINGS, Duration.ofMinutes(15), Duration.ofHours(6)));
  }
}
