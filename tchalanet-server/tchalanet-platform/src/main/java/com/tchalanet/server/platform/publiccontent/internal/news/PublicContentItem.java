package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSourceType;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentStatus;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSurface;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.lang.Nullable;

/**
 * Internal domain model for a public content item (internal or external/RSS).
 * Stored in cache; surfaces control which dashboard/page sees it.
 * RSS items have {@code targetSurfaces = Set.of()} (shown everywhere).
 */
public record PublicContentItem(
    String id,
    String sourceId,
    PublicContentSourceType sourceType,
    String title,
    @Nullable String content,
    @Nullable String contentHtml,
    @Nullable String imageUrl,
    @Nullable URI sourceUrl,
    @Nullable String author,
    PublicContentStatus status,
    /** Empty = visible on all surfaces. Non-empty = only on listed surfaces. */
    Set<PublicContentSurface> targetSurfaces,
    Instant publishedAt,
    @Nullable Instant expiresAt,
    List<String> categories) {

  public PublicContentItem withStatus(PublicContentStatus newStatus) {
    return new PublicContentItem(id, sourceId, sourceType, title, content, contentHtml,
        imageUrl, sourceUrl, author, newStatus, targetSurfaces, publishedAt, expiresAt, categories);
  }

  /** Whether this item should appear for a given surface. */
  public boolean visibleOn(PublicContentSurface surface) {
    return targetSurfaces == null || targetSurfaces.isEmpty()
        || targetSurfaces.contains(surface);
  }

  public boolean isPublishedAt(Instant now) {
    if (status != PublicContentStatus.PUBLISHED) return false;
    if (publishedAt != null && publishedAt.isAfter(now)) return false;
    if (expiresAt != null && !expiresAt.isAfter(now)) return false;
    return true;
  }
}
