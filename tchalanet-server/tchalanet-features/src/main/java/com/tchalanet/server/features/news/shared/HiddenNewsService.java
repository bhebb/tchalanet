package com.tchalanet.server.features.news.shared;

import com.tchalanet.server.common.cache.internal.CacheKeyBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HiddenNewsService {

  private final NewsCache newsCache;
  private final CacheKeyBuilder cacheKeyBuilder;

  private String hiddenKey() {
    return cacheKeyBuilder.newsHiddenKey();
  }

  public List<String> getHiddenIds() {
    return newsCache.getHidden(hiddenKey());
  }

  public void hide(String articleId) {
    newsCache.addHidden(hiddenKey(), articleId);
    log.info("News {} hidden in overlay cache", articleId);
  }

  public void show(String articleId) {
    newsCache.removeHidden(hiddenKey(), articleId);
    log.info("News {} unhidden in overlay cache", articleId);
  }

  public boolean isHidden(String articleId) {
    return getHiddenIds().contains(articleId);
  }
}
