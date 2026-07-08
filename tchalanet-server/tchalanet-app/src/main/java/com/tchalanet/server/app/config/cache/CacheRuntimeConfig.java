package com.tchalanet.server.app.config.cache;

import com.tchalanet.server.common.cache.CacheSpecProvider;
import com.tchalanet.server.common.cache.CacheToggle;
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
import org.springframework.core.Ordered;

@Configuration
// order below LOWEST_PRECEDENCE makes the caching advisor OUTER to the @Transactional advisor
// (which defaults to LOWEST_PRECEDENCE). Effect: on a method with both @Transactional and
// @CacheEvict, eviction runs AFTER the transaction commits — not before, which would let a
// concurrent read repopulate the cache with the pre-commit value. Without this the ordering is
// undefined (both LOWEST_PRECEDENCE).
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheRuntimeConfig {

  @Bean
  public CaffeineCacheManager caffeineCacheManager(List<CacheSpecProvider> specProviders) {
    return new CacheSpecAwareCaffeineCacheManager(specProviders, Duration.ofMinutes(5), 10_000);
  }

  @Bean
  @Primary
  public CacheManager cacheManager(
      CaffeineCacheManager caffeineCacheManager,
      @Qualifier("redisCacheManager") ObjectProvider<CacheManager> redisCacheManagerProvider,
      CacheToggle cacheToggle) {
    CacheManager redisCacheManager = redisCacheManagerProvider.getIfAvailable();
    CacheManager base =
        redisCacheManager != null
            ? new CombinedCacheManager(caffeineCacheManager, redisCacheManager)
            : caffeineCacheManager;
    // Wrap so every cache honours the runtime kill-switch (Ops can disable a cache without redeploy).
    return new ToggleableCacheManager(base, cacheToggle);
  }
}
