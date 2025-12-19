package com.tchalanet.server.core.draw.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DrawCacheConfig {

  @Bean
  public CacheSpecProvider drawCacheSpecProvider() {
    return () ->
        List.of(
            // Résumé des tirages par tenant, TTL L2 = 60s
            CacheSpec.of("tenant_draws_summary", Duration.ofHours(5)));
  }
}
