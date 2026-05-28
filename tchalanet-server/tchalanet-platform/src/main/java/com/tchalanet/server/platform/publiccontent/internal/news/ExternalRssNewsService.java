package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.platform.publiccontent.internal.news.provider.NewsProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Manages the external RSS feed cache.
 *
 * <p>Fix from spec: reading ({@link #getExternalSnapshot}) is cache-only and never triggers
 * a live HTTP fetch. Only {@link #refreshExternalSnapshot} fetches from the RSS provider.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRssNewsService {

  private final NewsProvider newsProvider;
  private final PublicContentCache cache;
  private final CacheKeyBuilder cacheKeyBuilder;

  /**
   * Return current cached external snapshot — cache-only, no HTTP call.
   * Returns empty list when cache is cold (before first scheduled refresh).
   */
  public List<PublicContentItem> getExternalSnapshot() {
    return cache.getExternalSnapshot(cacheKeyBuilder.newsExternalKey());
  }

  /**
   * Fetch RSS from provider and store in cache. Called by scheduler and admin force-refresh.
   * Failure is logged and stale cache is preserved (empty on cold start).
   */
  public List<PublicContentItem> refreshExternalSnapshot() {
    try {
      var items = newsProvider.fetchLatestNews();
      cache.putExternalSnapshot(cacheKeyBuilder.newsExternalKey(), items);
      log.info("publiccontent: refreshed external RSS snapshot ({} items)", items.size());
      return items;
    } catch (Exception e) {
      log.error("publiccontent: failed to refresh external RSS — keeping stale cache: {}", e.getMessage(), e);
      return getExternalSnapshot();
    }
  }
}
