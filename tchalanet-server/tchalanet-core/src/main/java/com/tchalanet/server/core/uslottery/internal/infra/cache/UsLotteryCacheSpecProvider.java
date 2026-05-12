package com.tchalanet.server.core.uslottery.internal.infra.cache;

import com.tchalanet.server.common.cache.internal.CacheSpec;
import com.tchalanet.server.common.cache.internal.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UsLotteryCacheSpecProvider implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(
            UsLotteryProviderRawCache.CACHE_NAME,
            Duration.ofMinutes(1),
            Duration.ofMinutes(5)));
  }
}
