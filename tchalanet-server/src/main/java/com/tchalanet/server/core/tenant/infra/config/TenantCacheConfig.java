package com.tchalanet.server.core.tenant.infra.config;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantCacheConfig {

  @Bean
  public CacheSpecProvider tenantCacheSpecProvider() {
    return () ->
        List.of(
            // Config tenant agrégée : 10–60 min, v1 = 30 min
            new CacheSpec("tenant_config", Duration.ofMinutes(30)),
            // Thème actif : même ordre de grandeur
            new CacheSpec("tenant_theme", Duration.ofMinutes(30)),
            // Limites / policies : TTL plus court, v1 = 15 min
            new CacheSpec("tenant_limits", Duration.ofMinutes(15))
        );
  }
}
