package com.tchalanet.server.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
  @Bean
  public Caffeine caffeine() {
    return Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(5));
  }

  @Bean
  public CacheManager cacheManager(Caffeine caffeine) {
    CaffeineCacheManager cm = new CaffeineCacheManager();
    cm.setCaffeine(caffeine);
    return cm;
  }
}
