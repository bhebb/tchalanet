package com.tchalanet.server.core.theme.infra.cache;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class ThemeCacheConfig {

    @Bean
    public CacheSpecProvider themeCacheSpecProvider() {
        return () -> List.of(
            CacheSpec.of("publishedThemeByTenant", Duration.ofMinutes(30))
        );
    }
}
