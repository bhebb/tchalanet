package com.tchalanet.server.core.pricing.internal.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PricingCacheSpecProvider implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(
            PricingCacheNames.TENANT_ODDS_LIST, Duration.ofMinutes(10), Duration.ofMinutes(30)),
        CacheSpec.of(
            PricingCacheNames.TENANT_ODDS_BY_VARIANT,
            Duration.ofMinutes(10),
            Duration.ofMinutes(30)));
  }
}
