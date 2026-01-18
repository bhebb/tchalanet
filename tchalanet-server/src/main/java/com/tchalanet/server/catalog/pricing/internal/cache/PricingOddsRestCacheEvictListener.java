package com.tchalanet.server.catalog.pricing.internal.cache;

import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.rest.core.event.AbstractRepositoryEventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PricingOddsRestCacheEvictListener extends AbstractRepositoryEventListener<PricingOddsEntity> {

  private final CacheManager cacheManager;

  @Override
  protected void onAfterCreate(PricingOddsEntity entity) {
    evict();
  }

  @Override
  protected void onAfterSave(PricingOddsEntity entity) {
    evict();
  }

  @Override
  protected void onAfterDelete(PricingOddsEntity entity) {
    evict();
  }

  private void evict() {
    var cache = cacheManager.getCache("pricing.odds");
    if (cache != null) cache.clear();
  }
}

