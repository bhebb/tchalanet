package com.tchalanet.server.core.draw.internal.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawCacheEvictor {

    public static final String SEARCH = "core.draw.summary.search";
    public static final String TODAY = "core.draw.today.search";
    public static final String UPCOMING = "core.draw.upcoming.search";
    public static final String NEXT = "core.draw.next.search";
    public static final String LATEST = "core.draw.latest_results.search";

    private final CacheManager cacheManager;

    /**
     * MVP strategy: broad eviction.
     *
     * <p>Later limitScopeRef: tenant-specific eviction once cache keys and cache abstraction support
     * tenant-level invalidation.
     */
    public void evictAll() {
        evict(SEARCH);
        evict(TODAY);
        evict(UPCOMING);
        evict(NEXT);
        evict(LATEST);
    }

    private void evict(String cacheName) {
        var cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            log.debug("draw.cache not configured cacheName={}", cacheName);
            return;
        }

        cache.clear();

        log.debug("draw.cache cleared cacheName={}", cacheName);
    }
}
