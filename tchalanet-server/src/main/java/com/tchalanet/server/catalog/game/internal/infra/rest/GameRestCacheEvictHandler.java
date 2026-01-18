package com.tchalanet.server.catalog.game.internal.infra.rest;

import com.tchalanet.server.catalog.game.internal.infra.persistence.GameJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.AfterDeleteEvent;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameRestCacheEvictHandler {

  private final CacheManager cacheManager;

  @EventListener
  public void afterCreate(AfterCreateEvent ev) {
    handleIfGame(ev.getSource());
  }

  @EventListener
  public void afterSave(AfterSaveEvent ev) {
    handleIfGame(ev.getSource());
  }

  @EventListener
  public void afterDelete(AfterDeleteEvent ev) {
    handleIfGame(ev.getSource());
  }

  private void handleIfGame(Object source) {
    if (!(source instanceof GameJpaEntity e)) return;

    // byId
    if (e.getId() != null) {
      evict("game.byId", e.getId().toString());
      log.debug("Requested evict game.byId for id={}", e.getId());
    }

    // byCode: normalize to lower-case trimmed (like resultslot keys)
    String code = e.getCode() == null ? null : e.getCode().trim().toLowerCase();
    if (code != null && !code.isBlank()) {
      evict("game.byCode", code);
      log.debug("Requested evict game.byCode for code={}", code);
    }

    // active list
    evict("game.active", null);
    log.debug("Requested clear game.active cache");
  }

  private void evict(String cacheName, String key) {
    var cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      log.debug("Cache manager returned null for cache name={}", cacheName);
      return;
    }
    try {
      if (key == null) {
        cache.clear();
        log.info("Cleared cache {}", cacheName);
      } else {
        cache.evict(key);
        log.info("Evicted cache {} key={}", cacheName, key);
      }
    } catch (Exception ex) {
      log.warn("Failed to evict cache {} key={} : {}", cacheName, key, ex.toString());
    }
  }
}
