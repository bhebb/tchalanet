package com.tchalanet.server.features.news;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NewsCacheConfig implements CacheSpecProvider {

    private final NewsConfigProperties newsConfigProperties;
    private final CacheKeyBuilder cacheKeyBuilder;

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(
                cacheKeyBuilder.newsExternalKey(),
                Duration.ofHours(newsConfigProperties.ttl().hours())),
            CacheSpec.of(
                cacheKeyBuilder.newsInternalKey(),
                Duration.ofHours(newsConfigProperties.ttl().hours())));
    }
}
