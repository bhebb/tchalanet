package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
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

  public boolean isHidden(PublicContentItem item) {
    if (item == null || item.id() == null) {
      return false;
    }
    var hidden = getHiddenIds();
    return hidden.contains(item.id()) || hidden.contains(publicId(item.id()));
  }

  private static String publicId(String id) {
    try {
      return UUID.fromString(id).toString();
    } catch (IllegalArgumentException ignored) {
      return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).toString();
    }
  }
}
