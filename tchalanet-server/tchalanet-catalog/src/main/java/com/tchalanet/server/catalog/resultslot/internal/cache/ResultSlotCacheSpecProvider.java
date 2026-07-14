package com.tchalanet.server.catalog.resultslot.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Result slot definitions (distinct from the calendar overrides in {@link
 * ResultSlotCalendarCacheSpecProvider}). Platform referential, edited very rarely and evicted on
 * write. Tier A: 30 min / 12 h.
 */
@Component
public class ResultSlotCacheSpecProvider implements CacheSpecProvider {

  private static final Duration L1 = Duration.ofMinutes(30);
  private static final Duration L2 = Duration.ofHours(12);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(ResultSlotCacheNames.ACTIVE, L1, L2),
        CacheSpec.of(ResultSlotCacheNames.BY_KEY, L1, L2),
        CacheSpec.of(ResultSlotCacheNames.BY_ID, L1, L2));
  }
}
