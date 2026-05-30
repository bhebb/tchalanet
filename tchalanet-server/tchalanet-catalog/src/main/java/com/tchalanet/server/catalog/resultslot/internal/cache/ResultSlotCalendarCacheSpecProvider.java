package com.tchalanet.server.catalog.resultslot.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Provider no-draw days change rarely and are SUPER_ADMIN managed (with explicit
 * cache eviction on every write). They can safely be cached for 24h.
 */
@Component
public class ResultSlotCalendarCacheSpecProvider implements CacheSpecProvider {

  private static final Duration TTL = Duration.ofHours(24);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(CacheSpec.of(ResultSlotCalendarCacheNames.BY_SLOT, TTL, TTL));
  }
}
