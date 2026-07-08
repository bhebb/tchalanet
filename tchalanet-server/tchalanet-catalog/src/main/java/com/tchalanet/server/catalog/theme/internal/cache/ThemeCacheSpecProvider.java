package com.tchalanet.server.catalog.theme.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/** Theme presets are a platform referential, edited very rarely and evicted on write. Tier A. */
@Component
public class ThemeCacheSpecProvider implements CacheSpecProvider {

  private static final Duration L1 = Duration.ofMinutes(30);
  private static final Duration L2 = Duration.ofHours(12);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(ThemeCacheNames.ACTIVE_PRESETS, L1, L2),
        CacheSpec.of(ThemeCacheNames.PRESET_BY_CODE, L1, L2));
  }
}
