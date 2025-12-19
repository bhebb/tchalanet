package com.tchalanet.server.core.tenant.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class TenantCacheConfig {

    @Bean
    public CacheSpecProvider tenantCacheSpecProvider() {
        return () ->
            List.of(
                // Hot paths
                CacheSpec.of("tenant_by_code", Duration.ofMinutes(60)), // code -> UUID
                CacheSpec.of("tenant_by_id", Duration.ofMinutes(60)) // id -> snapshot (optional)
            );
    }
}
