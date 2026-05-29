package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicContentCacheConfig implements CacheSpecProvider {

  private static final long DEFAULT_TTL_HOURS = 1L;

  private final PublicContentConfigProperties props;
  private final CacheKeyBuilder cacheKeyBuilder;

  @Override
  public List<CacheSpec> cacheSpecs() {
    long hours = props.ttl() != null ? props.ttl().hours() : DEFAULT_TTL_HOURS;
    Duration ttl = Duration.ofHours(hours);
    return List.of(
        CacheSpec.of(cacheKeyBuilder.newsExternalKey(), ttl),
        CacheSpec.of(cacheKeyBuilder.newsInternalKey(), ttl),
        CacheSpec.of(cacheKeyBuilder.newsHiddenKey(), Duration.ofDays(30)));
  }
}
