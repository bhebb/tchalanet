package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HiddenPublicContentService {

  private final PublicContentCache cache;
  private final CacheKeyBuilder cacheKeyBuilder;

  public List<String> getHiddenIds() {
    return cache.getHidden(cacheKeyBuilder.newsHiddenKey());
  }

  public void hide(String itemId) {
    cache.addHidden(cacheKeyBuilder.newsHiddenKey(), itemId);
    log.info("publiccontent: item {} hidden", itemId);
  }

  public void show(String itemId) {
    cache.removeHidden(cacheKeyBuilder.newsHiddenKey(), itemId);
    log.info("publiccontent: item {} shown", itemId);
  }

  public boolean isHidden(String itemId) {
    return getHiddenIds().contains(itemId);
  }
}
