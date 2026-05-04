package com.tchalanet.server.core.uslottery.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
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
