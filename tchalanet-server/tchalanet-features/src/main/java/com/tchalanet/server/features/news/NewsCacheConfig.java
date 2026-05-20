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

    private static final long DEFAULT_TTL_HOURS = 1L;

    @Override
    public List<CacheSpec> cacheSpecs() {
        long hours = newsConfigProperties.ttl() != null
            ? newsConfigProperties.ttl().hours()
            : DEFAULT_TTL_HOURS;
        return List.of(
            CacheSpec.of(cacheKeyBuilder.newsExternalKey(), Duration.ofHours(hours)),
            CacheSpec.of(cacheKeyBuilder.newsInternalKey(), Duration.ofHours(hours)));
    }
}
