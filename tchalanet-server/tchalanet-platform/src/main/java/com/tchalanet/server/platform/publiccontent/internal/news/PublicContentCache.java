package com.tchalanet.server.platform.publiccontent.internal.news;

import java.util.List;

/** Cache contract for public content snapshots and hidden overlays. */
public interface PublicContentCache {

  /** Read internal content snapshot (never refetches). */
  List<PublicContentItem> getInternalSnapshot(String cacheKey);

  /** Write/replace internal content snapshot. */
  void putInternalSnapshot(String cacheKey, List<PublicContentItem> items);

  /** Read external RSS snapshot (never refetches). */
  List<PublicContentItem> getExternalSnapshot(String cacheKey);

  /** Write/replace external RSS snapshot. */
  void putExternalSnapshot(String cacheKey, List<PublicContentItem> items);

  /** Add an article ID to the hidden overlay. */
  void addHidden(String cacheKey, String articleId);

  /** Return all hidden article IDs. */
  List<String> getHidden(String cacheKey);

  /** Remove an article ID from the hidden overlay. */
  void removeHidden(String cacheKey, String articleId);
}
