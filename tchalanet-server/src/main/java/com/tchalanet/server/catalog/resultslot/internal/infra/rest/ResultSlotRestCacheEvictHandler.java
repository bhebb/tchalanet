package com.tchalanet.server.catalog.resultslot.internal.infra.rest;

import com.tchalanet.server.catalog.resultslot.internal.infra.persistence.ResultSlotJpaEntity;
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
public class ResultSlotRestCacheEvictHandler {

  private final CacheManager cacheManager;

  @EventListener
  public void afterCreate(AfterCreateEvent ev) {
    handleIfResultSlot(ev.getSource());
  }

  @EventListener
  public void afterSave(AfterSaveEvent ev) {
    handleIfResultSlot(ev.getSource());
  }

  @EventListener
  public void afterDelete(AfterDeleteEvent ev) {
    handleIfResultSlot(ev.getSource());
  }

  private void handleIfResultSlot(Object source) {
    if (!(source instanceof ResultSlotJpaEntity e)) return;

    // evict byKey
    String key = (e.getSlotKey() == null) ? null : e.getSlotKey().trim().toLowerCase();
    log.info("Evicting cache for slot key and active results: {}", key);
    evict("resultslot.byKey", key);

    // evict listActive if cached
    evict("resultslot.active", null);
  }

  private void evict(String cacheName, String key) {
    var cache = cacheManager.getCache(cacheName);
    if (cache == null) return;
    if (key == null) cache.clear();
    else cache.evict(key);
  }
}
