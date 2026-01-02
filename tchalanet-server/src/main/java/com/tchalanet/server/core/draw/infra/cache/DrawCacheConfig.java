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
            // Résumé des tirages par tenant
            CacheSpec.of(
                "tenant_draws_summary",
                Duration.ofSeconds(60), // L2
                Duration.ofSeconds(10) // L1
                ),

            // RAW provider NY / FL (JSON brut)
            CacheSpec.of(
                "uslottery_provider_raw",
                Duration.ofMinutes(60), // L2
                Duration.ofMinutes(30) // L1
                ));
  }
}
