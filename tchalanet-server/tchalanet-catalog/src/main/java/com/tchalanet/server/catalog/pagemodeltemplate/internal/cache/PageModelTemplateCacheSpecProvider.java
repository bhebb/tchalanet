package com.tchalanet.server.catalog.pagemodeltemplate.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Page model templates are a rendering referential, edited very rarely and evicted on write. Tier A
 * (30 min / 12 h) for the by-id / logical-id / visible-list reads. The paginated SEARCH cache gets
 * a short TTL: many keys, low reuse.
 */
@Component
public class PageModelTemplateCacheSpecProvider implements CacheSpecProvider {

  private static final Duration L1 = Duration.ofMinutes(30);
  private static final Duration L2 = Duration.ofHours(12);

  private static final Duration SEARCH_L1 = Duration.ofMinutes(2);
  private static final Duration SEARCH_L2 = Duration.ofMinutes(5);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(PageModelTemplateCacheNames.BY_ID, L1, L2),
        CacheSpec.of(PageModelTemplateCacheNames.BY_LOGICAL_ID, L1, L2),
        CacheSpec.of(PageModelTemplateCacheNames.VISIBLE_LIST, L1, L2),
        CacheSpec.of(PageModelTemplateCacheNames.SEARCH, SEARCH_L1, SEARCH_L2));
  }
}
