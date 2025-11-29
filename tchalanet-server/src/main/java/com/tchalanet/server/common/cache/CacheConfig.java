package com.tchalanet.server.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;

@Configuration
@EnableCaching
public class CacheConfig {
  @Bean
  public Caffeine<Object, Object> caffeine() {
    return Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(5));
  }

  @Bean
  public CaffeineCacheManager caffeineCacheManager(Caffeine<Object, Object> caffeine) {
    CaffeineCacheManager cm = new CaffeineCacheManager();
    cm.setCaffeine(caffeine);
    return cm;
  }

  @Bean
  @Primary
  public CacheManager cacheManager(
      CaffeineCacheManager caffeineCacheManager, @Nullable CacheManager redisCacheManager) {
    // If Redis is available, prefer a composite manager (local Caffeine + remote Redis).
    // Otherwise fall back to the local Caffeine cache manager only.
    if (redisCacheManager != null) {
      return new com.tchalanet.server.common.cache.CombinedCacheManager(
          caffeineCacheManager, redisCacheManager);
    }

    return caffeineCacheManager;
  }
}
