package com.tchalanet.server.common.settings;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppSettingCacheConfig {

  @Bean
  public CacheSpecProvider appSettingsCacheSpecProvider() {
    return () -> List.of(CacheSpec.of("app_settings_resolved", Duration.ofMinutes(10)));
  }
}
