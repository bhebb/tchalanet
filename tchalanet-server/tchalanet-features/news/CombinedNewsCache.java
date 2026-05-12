package com.tchalanet.server.features.news;

import com.tchalanet.server.features.news.shared.LotteryNewsModels.LotteryNewsFeedSnapshot;
import com.tchalanet.server.features.news.shared.NewsCache;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CombinedNewsCache implements NewsCache {

  private final CacheManager cacheManager;

  private <T> T getValue(String cacheName, Class<T> type) {
    var cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      log.warn("Cache {} not found", cacheName);
      return null;
    }
    return cache.get(cacheName, type);
  }

  private void putValue(String cacheName, Object value) {
    var cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      log.warn("Cache {} not found", cacheName);
      return;
    }
    cache.put(cacheName, value);
  }

  @Override
  public LotteryNewsFeedSnapshot getLatestNews(String cacheName) {
    var snapshot = getValue(cacheName, LotteryNewsFeedSnapshot.class);
    if (snapshot == null) {
      return LotteryNewsFeedSnapshot.empty();
    }
    return snapshot;
  }

  @Override
  public void putLatestNews(String cacheName, LotteryNewsFeedSnapshot snapshot) {
    putValue(cacheName, snapshot);
  }

  @Override
  public List<String> getHidden(String cacheName) {
    var current = getValue(cacheName, java.util.List.class);
    if (current == null) {
      return java.util.List.of();
    }
    // on ne fait pas confiance au type brut, on recopie
    return new java.util.ArrayList<>((java.util.List<String>) current);
  }

  @Override
  public void addHidden(String cacheName, String articleId) {
    var current = new java.util.LinkedHashSet<>(getHidden(cacheName));
    current.add(articleId);
    putValue(cacheName, new java.util.ArrayList<>(current));
  }

  @Override
  public void removeHidden(String cacheName, String articleId) {
    var current = new java.util.LinkedHashSet<>(getHidden(cacheName));
    if (current.remove(articleId)) {
      putValue(cacheName, new java.util.ArrayList<>(current));
    }
  }
}
