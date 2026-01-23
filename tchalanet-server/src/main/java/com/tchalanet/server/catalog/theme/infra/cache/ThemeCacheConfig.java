package com.tchalanet.server.catalog.theme.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThemeCacheConfig {

  @Bean
  public CacheSpecProvider themeCacheSpecProvider() {
    return () -> List.of(CacheSpec.of("publishedThemeByTenant", Duration.ofMinutes(30)));
  }
}
