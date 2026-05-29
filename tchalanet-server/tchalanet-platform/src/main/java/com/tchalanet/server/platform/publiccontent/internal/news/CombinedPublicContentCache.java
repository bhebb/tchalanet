package com.tchalanet.server.platform.publiccontent.internal.news;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CombinedPublicContentCache implements PublicContentCache {

  private final CacheManager cacheManager;

  @Override
  public List<PublicContentItem> getInternalSnapshot(String cacheKey) {
    return getItems(cacheKey);
  }

  @Override
  public void putInternalSnapshot(String cacheKey, List<PublicContentItem> items) {
    put(cacheKey, items);
  }

  @Override
  public List<PublicContentItem> getExternalSnapshot(String cacheKey) {
    return getItems(cacheKey);
  }

  @Override
  public void putExternalSnapshot(String cacheKey, List<PublicContentItem> items) {
    put(cacheKey, items);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getHidden(String cacheName) {
    var cache = cacheManager.getCache(cacheName);
    if (cache == null) return List.of();
    var raw = cache.get(cacheName, List.class);
    if (raw == null) return List.of();
    return new ArrayList<>((List<String>) raw);
  }

  @Override
  public void addHidden(String cacheKey, String articleId) {
    var current = new ArrayList<>(getHidden(cacheKey));
    if (!current.contains(articleId)) {
      current.add(articleId);
      put(cacheKey, current);
    }
  }

  @Override
  public void removeHidden(String cacheKey, String articleId) {
    var current = new ArrayList<>(getHidden(cacheKey));
    current.remove(articleId);
    put(cacheKey, current);
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private List<PublicContentItem> getItems(String cacheName) {
    var cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      log.warn("publiccontent: cache {} not found", cacheName);
      return List.of();
    }
    var raw = cache.get(cacheName, List.class);
    if (raw == null) return List.of();
    return (List<PublicContentItem>) raw;
  }

  private void put(String cacheName, Object value) {
    var cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      log.warn("publiccontent: cache {} not found — skipping put", cacheName);
      return;
    }
    cache.put(cacheName, value);
  }
}
