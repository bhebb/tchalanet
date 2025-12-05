package com.tchalanet.server.features.news.config;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class NewsCacheConfig {

    private final NewsConfigProperties newsConfigProperties;
    private final CacheKeyBuilder cacheKeyBuilder;

    @Bean
    public CacheSpecProvider newsCacheSpecProvider() {
        return () -> List.of(
            new CacheSpec(cacheKeyBuilder.newsExternalKey(), Duration.ofHours(newsConfigProperties.getTtl().getHours())),
            new CacheSpec(cacheKeyBuilder.newsInternalKey(), Duration.ofHours(newsConfigProperties.getTtl().getHours())));
    }
}
