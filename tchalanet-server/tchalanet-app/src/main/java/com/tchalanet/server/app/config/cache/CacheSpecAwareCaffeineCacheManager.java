package com.tchalanet.server.app.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

public class CacheSpecAwareCaffeineCacheManager extends CaffeineCacheManager {

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
    if (specProviders == null) return defaultTtl;
    return specProviders.stream()
        .flatMap(p -> p.cacheSpecs().stream())
        .filter(s -> s.name().equals(cacheName))
        .findFirst()
        .map(CacheSpec::ttlL1)
        .orElse(defaultTtl);
  }
}
