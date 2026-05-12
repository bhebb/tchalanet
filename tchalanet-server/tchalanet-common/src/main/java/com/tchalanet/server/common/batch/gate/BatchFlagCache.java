package com.tchalanet.server.common.batch.gate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchFlagCache {

    public static final String CACHE_NAME = "platform.batch.flags.by_key";

    private final CacheManager cacheManager;

    /**
     * Get a Boolean flag from the cache. Returns null when missing and loader returns null.
     */
    public @Nullable Boolean getBool(String cacheKey, Supplier<Boolean> loaderSupplier) {
        var cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return safeLoad(loaderSupplier);
        }

        try {
            var cached = cache.get(cacheKey, Boolean.class);
            if (cached != null) {
                return cached;
            }

            var loaded = safeLoad(loaderSupplier);
            if (loaded != null) {
                cache.put(cacheKey, loaded);
            }

            return loaded;
        } catch (Exception ex) {
            log.warn("batch.flagcache.get.failed cacheKey={} cause={}", cacheKey, ex.getLocalizedMessage(), ex);
        }
        return safeLoad(loaderSupplier);
    }

    public void put(String cacheKey, boolean value) {
        var cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return;
        try {
            cache.put(cacheKey, value);
        } catch (Exception ex) {
            log.warn("batch.flagcache.put.failed cacheKey={} cause={}", cacheKey, ex.toString());
        }
    }

    public void evict(String cacheKey) {
        var cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return;
        try {
            cache.evict(cacheKey);
        } catch (Exception ex) {
            log.warn("batch.flagcache.evict.failed cacheKey={} cause={}", cacheKey, ex.toString());
        }
    }

    public void clear() {
        var cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return;
        try {
            cache.clear();
        } catch (Exception ex) {
            log.warn("batch.flagcache.clear.failed cause={}", ex.toString());
        }
    }

    private @Nullable Boolean safeLoad(Supplier<Boolean> loaderSupplier) {
        try {
            return loaderSupplier.get();
        } catch (Exception ex) {
            log.warn("batch.flagcache.loader.failed cause={}", ex.toString());
            return null;
        }
    }
}
