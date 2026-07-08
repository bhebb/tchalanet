package com.tchalanet.server.core.drawresult.internal.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultCacheEvictor {

    private final CacheManager cacheManager;

    public void evictAll() {
        evict(DrawResultCacheNames.BY_ID);
        evict(DrawResultCacheNames.ID_BY_SLOT_OCCURRED);
    }

    private void evict(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.debug("drawresult cache not configured: {}", cacheName);
            return;
        }
        cache.clear();
    }
}
