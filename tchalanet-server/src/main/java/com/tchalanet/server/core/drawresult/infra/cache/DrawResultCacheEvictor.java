package com.tchalanet.server.core.drawresult.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultCacheEvictor {

    public static final String BY_ID = "core.drawresult.by_id";
    public static final String ID_BY_SLOT_OCCURRED = "core.drawresult.id.by_slot_occurred";
    public static final String LATEST_BY_SLOT = "core.drawresult.latest.by_slot";

    private final CacheManager cacheManager;

    public void evictAll() {
        evict(BY_ID);
        evict(ID_BY_SLOT_OCCURRED);
        evict(LATEST_BY_SLOT);
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
