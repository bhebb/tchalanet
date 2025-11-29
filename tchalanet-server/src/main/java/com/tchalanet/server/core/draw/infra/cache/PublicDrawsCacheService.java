package com.tchalanet.server.core.draw.infra.cache;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class PublicDrawsCacheService {

  private final CacheManager cacheManager;
  private final CacheKeyBuilder keyBuilder;

  public PublicDrawsCacheService(CacheManager cacheManager, CacheKeyBuilder keyBuilder) {
    this.cacheManager = cacheManager;
    this.keyBuilder = keyBuilder;
  }

  public DrawSummary getFromCache(UUID tenantId) {
    var key = keyBuilder.tenantDrawsSummaryKey(tenantId);
    var cache = cacheManager.getCache("publicDraws");
    if (cache == null) return null;
    return cache.get(key, DrawSummary.class);
  }

  public void put(UUID tenantId, DrawSummary summary) {
    var key = keyBuilder.tenantDrawsSummaryKey(tenantId);
    var cache = cacheManager.getCache("publicDraws");
    if (cache != null) cache.put(key, summary);
  }

  public void evict(UUID tenantId) {
    var key = keyBuilder.tenantDrawsSummaryKey(tenantId);
    var cache = cacheManager.getCache("publicDraws");
    if (cache != null) cache.evict(key);
  }
}
