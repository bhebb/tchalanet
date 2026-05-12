package com.tchalanet.server.common.cache.internal;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CaffeineCacheManager caffeineCacheManager(List<CacheSpecProvider> specProviders) {
    // defaults si pas de spec
    return new CacheSpecAwareCaffeineCacheManager(specProviders, Duration.ofMinutes(5), 10_000);
  }

  @Bean
  @Primary
  public CacheManager cacheManager(
      CaffeineCacheManager caffeineCacheManager,
      @Qualifier("redisCacheManager") ObjectProvider<CacheManager> redisCacheManagerProvider) {
    CacheManager redisCacheManager = redisCacheManagerProvider.getIfAvailable();
    if (redisCacheManager != null) {
      return new CombinedCacheManager(caffeineCacheManager, redisCacheManager);
    }
    return caffeineCacheManager;
  }
}
