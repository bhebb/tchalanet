package com.tchalanet.server.app.batch.gate;

import static com.tchalanet.server.app.batch.gate.BatchFlagCache.CACHE_NAME;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BatchCacheConfig implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(CACHE_NAME, Duration.ofSeconds(15), Duration.ofMinutes(2)));
  }
}
