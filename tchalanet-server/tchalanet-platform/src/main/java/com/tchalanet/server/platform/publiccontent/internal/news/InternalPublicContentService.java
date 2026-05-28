package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Manages platform-authored (internal) public content items in cache.
 *
 * <p>Fix from spec: save() does a full-snapshot upsert-by-id, not a single-item replacement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InternalPublicContentService {

  private final PublicContentCache cache;
  private final CacheKeyBuilder cacheKeyBuilder;

  public List<PublicContentItem> findAll() {
    return cache.getInternalSnapshot(cacheKeyBuilder.newsInternalKey());
  }

  /** Returns only PUBLISHED items within the publication window. */
  public List<PublicContentItem> findPublished(Instant now) {
    return findAll().stream()
        .filter(item -> item.isPublishedAt(now))
        .toList();
  }

  public Optional<PublicContentItem> findById(String id) {
    return findAll().stream().filter(i -> id.equals(i.id())).findFirst();
  }

  /**
   * Upsert by ID — updates existing item or appends new one.
   * Does NOT replace the entire snapshot with a single item (bug fix).
   */
  public PublicContentItem save(PublicContentItem item) {
    var current = new ArrayList<>(findAll());
    boolean found = false;
    for (int i = 0; i < current.size(); i++) {
      if (current.get(i).id().equals(item.id())) {
        current.set(i, item);
        found = true;
        break;
      }
    }
    if (!found) {
      current.add(item);
    }
    cache.putInternalSnapshot(cacheKeyBuilder.newsInternalKey(), current);
    return item;
  }

  public PublicContentItem changeStatus(String id, PublicContentStatus newStatus) {
    var existing = findById(id)
        .orElseThrow(() -> new NoSuchElementException("Content item not found: " + id));
    return save(existing.withStatus(newStatus));
  }

  /** Remove an item permanently from the internal snapshot. */
  public void delete(String id) {
    var updated = findAll().stream().filter(i -> !id.equals(i.id())).toList();
    cache.putInternalSnapshot(cacheKeyBuilder.newsInternalKey(), updated);
  }
}
