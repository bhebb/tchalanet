package com.tchalanet.server.core.uslottery.internal.infra.cache;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.tchalanet.server.common.cache.internal.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsLotteryProviderRawCache {

    public static final String CACHE_NAME = "infra.uslottery.provider_raw";

    private static final Logger log = LoggerFactory.getLogger(UsLotteryProviderRawCache.class);

    private final CacheManager cacheManager;
    private final CacheKeyBuilder keyBuilder;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public String getOrFetch(
        String provider,
        LocalDate drawDate,
        String queryHash,
        Supplier<String> fetcher
    ) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider is required");
        }
        if (drawDate == null) {
            throw new IllegalArgumentException("drawDate is required");
        }
        if (queryHash == null || queryHash.isBlank()) {
            throw new IllegalArgumentException("queryHash is required");
        }
        if (fetcher == null) {
            throw new IllegalArgumentException("fetcher is required");
        }

        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.debug("Cache '{}' not configured, calling provider directly", CACHE_NAME);
            return fetcher.get();
        }

        String key = keyBuilder.usLotteryProviderRawKey(provider, drawDate, queryHash);
        if (key == null || key.isBlank()) {
            log.debug("Provider raw cache key builder returned blank, calling provider directly");
            return fetcher.get();
        }

        String existing = cache.get(key, String.class);
        if (existing != null) {
            log.debug("Provider raw cache hit provider={} date={} queryHash={}", provider, drawDate, queryHash);
            return existing;
        }

        Object lock = locks.computeIfAbsent(key, ignored -> new Object());

        synchronized (lock) {
            try {
                existing = cache.get(key, String.class);
                if (existing != null) {
                    log.debug("Provider raw cache filled by another thread key={}", key);
                    return existing;
                }

                log.debug("Provider raw cache miss provider={} date={} queryHash={}", provider, drawDate, queryHash);

                String value = fetcher.get();
                if (value == null) {
                    log.debug("Provider returned null; not caching provider={} date={} queryHash={}", provider, drawDate, queryHash);
                    return null;
                }

                try {
                    cache.put(key, value);
                } catch (Exception e) {
                    log.warn("Failed to put provider raw cache key={}: {}", key, e.getMessage());
                }

                return value;
            } finally {
                locks.computeIfPresent(key, (k, v) -> v == lock ? null : v);
            }
        }
    }

    public Optional<String> getIfPresent(String provider, LocalDate drawDate, String queryHash) {
        if (provider == null || drawDate == null || queryHash == null) {
            return Optional.empty();
        }

        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return Optional.empty();
        }

        String key = keyBuilder.usLotteryProviderRawKey(provider, drawDate, queryHash);
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(cache.get(key, String.class));
    }

    public void put(String provider, LocalDate drawDate, String queryHash, String value) {
        if (provider == null || drawDate == null || queryHash == null) {
            throw new IllegalArgumentException("provider, drawDate and queryHash are required");
        }
        if (value == null) {
            return;
        }

        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return;
        }

        String key = keyBuilder.usLotteryProviderRawKey(provider, drawDate, queryHash);
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            cache.put(key, value);
        } catch (Exception e) {
            log.warn("Failed to put provider raw cache key={}: {}", key, e.getMessage());
        }
    }

    public void evict(String provider, LocalDate drawDate, String queryHash) {
        if (provider == null || drawDate == null || queryHash == null) {
            return;
        }

        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return;
        }

        String key = keyBuilder.usLotteryProviderRawKey(provider, drawDate, queryHash);
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            cache.evict(key);
        } catch (Exception e) {
            log.warn("Failed to evict provider raw cache key={}: {}", key, e.getMessage());
        }
    }
}
