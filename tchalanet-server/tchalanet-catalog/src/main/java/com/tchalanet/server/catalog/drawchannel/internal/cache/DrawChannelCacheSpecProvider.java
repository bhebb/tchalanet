package com.tchalanet.server.catalog.drawchannel.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/** Per-tenant draw channel config, changed rarely and evicted on write. Tier B: 15 min / 6 h. */
@Component
public class DrawChannelCacheSpecProvider implements CacheSpecProvider {

  private static final Duration L1 = Duration.ofMinutes(15);
  private static final Duration L2 = Duration.ofHours(6);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(DrawChannelCacheNames.BY_TENANT, L1, L2),
        CacheSpec.of(DrawChannelCacheNames.BY_ID, L1, L2),
        CacheSpec.of(DrawChannelCacheNames.BY_TENANT_GAME_MAP, L1, L2),
        CacheSpec.of(DrawChannelCacheNames.CALENDAR_ROWS, L1, L2));
  }
}
