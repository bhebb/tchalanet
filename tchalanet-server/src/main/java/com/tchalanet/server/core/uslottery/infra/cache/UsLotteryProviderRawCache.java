package com.tchalanet.server.core.uslottery.infra.cache;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
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

  private final CacheManager cacheManager;
  private final CacheKeyBuilder keyBuilder;

  private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

  private static final Logger log = LoggerFactory.getLogger(UsLotteryProviderRawCache.class);

  public String getOrFetch(
      String provider, LocalDate drawDate, String queryHash, Supplier<String> fetcher) {
    if (provider == null) throw new IllegalArgumentException("provider is required");
    if (drawDate == null) throw new IllegalArgumentException("drawDate is required");
    if (queryHash == null) throw new IllegalArgumentException("queryHash is required");
    if (fetcher == null) throw new IllegalArgumentException("fetcher is required");

    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache == null) {
      log.info("Cache '{}' not configured, calling fetcher directly", CACHE_NAME);
      return fetcher.get();
    }

    String key = keyBuilder.usLotteryProviderRawKey(provider, drawDate, queryHash);
    if (key == null) {
      log.info("Cache slotKey builder returned null, calling fetcher directly");
      return fetcher.get();
    }

    // first fast-path: try to read without locking
    String existing = cache.get(key, String.class);
    if (existing != null) {
      log.info("Cache hit for provider={} date={} slotKey={}", provider, drawDate, key);
      return existing;
    }

    // avoid stampede: per-slotKey lock
    Object lock = locks.computeIfAbsent(key, k -> new Object());
    synchronized (lock) {
      try {
        // double-check after acquiring lock
        existing = cache.get(key, String.class);
        if (existing != null) {
          log.info("Cache filled by other thread for slotKey={}", key);
          return existing;
        }

        log.info("Cache miss for slotKey={}, invoking fetcher", key);
        String value = fetcher.get();
        if (value != null) {
          try {
            cache.put(key, value);
            log.info("Stored value in cache for slotKey={}", key);
          } catch (Exception e) {
            log.warn("Failed to put value into cache for slotKey={}: {}", key, e.getMessage());
          }
        } else {
          log.info("Fetcher returned null for slotKey={}; not caching", key);
        }
        return value;
      } finally {
        // clean up lock entry to avoid memory leak
        locks.remove(key, lock);
      }
    }
  }

  public Optional<String> getIfPresent(String provider, LocalDate drawDate, String queryHash) {
    if (provider == null || drawDate == null || queryHash == null) return Optional.empty();
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache == null) return Optional.empty();
    String key = keyBuilder.usLotteryProviderRawKey(provider, drawDate, queryHash);
    if (key == null) return Optional.empty();
    return Optional.ofNullable(cache.get(key, String.class));
  }

  /**
   * Put a value in the cache for the given provider/drawDate/queryHash. Does nothing if cache not
   * configured, slotKey cannot be built or value is null.
   */
  public void put(String provider, LocalDate drawDate, String queryHash, String value) {
    if (provider == null || drawDate == null || queryHash == null) {
      throw new IllegalArgumentException("provider, drawDate and queryHash are required");
    }
    if (value == null) {
      log.info(
          "Not caching null value for provider={} date={} queryHash={}",
          provider,
          drawDate,
          queryHash);
      return;
    }

    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache == null) {
      log.info(
          "Cache '{}' not configured, skipping put for provider={} date={}",
          CACHE_NAME,
          provider,
          drawDate);
      return;
    }

    String key = keyBuilder.usLotteryProviderRawKey(provider, drawDate, queryHash);
    if (key == null) {
      log.info("Cache slotKey builder returned null, skipping put");
      return;
    }

    try {
      cache.put(key, value);
      log.info("Put value in cache for slotKey={}", key);
    } catch (Exception e) {
      log.warn("Failed to put value into cache for slotKey={}: {}", key, e.getMessage());
    }
  }

  public void evict(String provider, LocalDate drawDate, String queryHash) {
    if (provider == null || drawDate == null || queryHash == null) return;
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache == null) return;

    String key = keyBuilder.usLotteryProviderRawKey(provider, drawDate, queryHash);
    if (key == null) return;
    try {
      cache.evict(key);
      log.info("Evicted cache slotKey={}", key);
    } catch (Exception e) {
      log.warn("Failed to evict cache slotKey={}: {}", key, e.getMessage());
    }
  }
}
