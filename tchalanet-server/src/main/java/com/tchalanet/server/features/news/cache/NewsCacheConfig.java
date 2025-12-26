package com.tchalanet.server.features.news.cache;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import com.tchalanet.server.features.news.config.NewsConfigProperties;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NewsCacheConfig {

  private final NewsConfigProperties newsConfigProperties;
  private final CacheKeyBuilder cacheKeyBuilder;

  @Bean
  public CacheSpecProvider newsCacheSpecProvider() {
    return () ->
        List.of(
            CacheSpec.of(
                cacheKeyBuilder.newsExternalKey(),
                Duration.ofHours(newsConfigProperties.getTtl().getHours())),
            CacheSpec.of(
                cacheKeyBuilder.newsInternalKey(),
                Duration.ofHours(newsConfigProperties.getTtl().getHours())));
  }
}
