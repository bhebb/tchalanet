package com.tchalanet.server.app.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

public class CacheSpecAwareCaffeineCacheManager extends CaffeineCacheManager {

  private static final Logger log =
      LoggerFactory.getLogger(CacheSpecAwareCaffeineCacheManager.class);

  private final List<CacheSpecProvider> specProviders;
  private final Duration defaultTtl;
  private final long defaultMaxSize;

  public CacheSpecAwareCaffeineCacheManager(
      List<CacheSpecProvider> specProviders, Duration defaultTtl, long defaultMaxSize) {
    this.specProviders = specProviders;
    this.defaultTtl = defaultTtl;
    this.defaultMaxSize = defaultMaxSize;
  }

  @Override
  protected Cache createCaffeineCache(String name) {
    Duration ttl = resolveTtlL1(name);
    var caffeine = Caffeine.newBuilder().maximumSize(defaultMaxSize).expireAfterWrite(ttl);
    return new CaffeineCache(name, caffeine.build());
  }

  private Duration resolveTtlL1(String cacheName) {
    if (specProviders != null) {
      var declared =
          specProviders.stream()
              .flatMap(p -> p.cacheSpecs().stream())
              .filter(s -> s.name().equals(cacheName))
              .findFirst()
              .map(CacheSpec::ttlL1);
      if (declared.isPresent()) {
        return declared.get();
      }
    }
    // Guard: a cache reaching here has no CacheSpecProvider and silently uses the generic default.
    // Warned once per name (createCaffeineCache is called once per cache). A hard startup failure
    // would need scanning every @Cacheable annotation — deferred.
    log.warn(
        "Cache '{}' has no CacheSpecProvider — using default L1 TTL {}. Declare it in a"
            + " CacheSpecProvider (see docs/conventions/cache.md §8).",
        cacheName,
        defaultTtl);
    return defaultTtl;
  }
}
