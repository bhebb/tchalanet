package com.tchalanet.server.core.drawresult.internal.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultCacheEvictor {

  private final CacheManager cacheManager;

  public void evictAll() {
    evict("drawresult.byId");
    evict("drawresult.id.bySlotOccurred");
    evict("publicdraw.latest");
    evict("publicdraw.one");
  }

  private void evict(String cacheName) {
    var c = cacheManager.getCache(cacheName);
    if (c != null) c.clear();
  }
}
