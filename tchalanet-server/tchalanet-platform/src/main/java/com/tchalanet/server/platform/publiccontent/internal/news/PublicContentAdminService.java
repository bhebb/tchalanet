package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSourceType;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentStatus;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSurface;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Write operations for platform admin management of public content.
 * Fix from spec: forceRefresh() triggers real RSS refresh (not empty snapshot).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicContentAdminService {

  private final InternalPublicContentService internalService;
  private final ExternalRssNewsService externalRssService;
  private final HiddenPublicContentService hiddenService;
  private final Clock clock;

  /** List internal items and visible external RSS items for admin management. */
  public List<PublicContentItem> listAll() {
    var internal = internalService.findAll().stream()
        .sorted(Comparator.comparing(PublicContentItem::publishedAt).reversed())
        .toList();
    var external = externalRssService.getExternalSnapshot().stream()
        .filter(item -> !hiddenService.isHidden(item))
        .sorted(Comparator.comparing(PublicContentItem::publishedAt).reversed())
        .toList();
    return java.util.stream.Stream.concat(internal.stream(), external.stream()).toList();
  }

  public PublicContentItem upsert(
      String id,
      String title,
      String content,
      String contentHtml,
      String imageUrl,
      String sourceUrl,
      PublicContentStatus status,
      Set<PublicContentSurface> targetSurfaces,
      Instant publishedAt,
      Instant expiresAt) {

    Instant now = Instant.now(clock);
    String resolvedId = id != null ? id : UUID.randomUUID().toString();
    Instant resolvedPublishedAt = publishedAt != null ? publishedAt : now;
    PublicContentStatus resolvedStatus = status != null ? status : PublicContentStatus.DRAFT;

    var existing = internalService.findById(resolvedId);

    PublicContentItem item = existing.map(e ->
        new PublicContentItem(
            resolvedId, "internal", PublicContentSourceType.INTERNAL,
            title != null ? title : e.title(),
            content != null ? content : e.content(),
            contentHtml != null ? contentHtml : e.contentHtml(),
            imageUrl != null ? imageUrl : e.imageUrl(),
            sourceUrl != null ? java.net.URI.create(sourceUrl) : e.sourceUrl(),
            "Tchalanet",
            resolvedStatus,
            targetSurfaces != null ? targetSurfaces : e.targetSurfaces(),
            resolvedPublishedAt,
            expiresAt != null ? expiresAt : e.expiresAt(),
            List.of())
    ).orElseGet(() ->
        new PublicContentItem(
            resolvedId, "internal", PublicContentSourceType.INTERNAL,
            title, content, contentHtml, imageUrl,
            sourceUrl != null ? java.net.URI.create(sourceUrl) : null,
            "Tchalanet",
            resolvedStatus,
            targetSurfaces != null ? targetSurfaces : Set.of(),
            resolvedPublishedAt,
            expiresAt,
            List.of())
    );

    var saved = internalService.save(item);
    // Creating or republishing removes from hidden overlay
    hiddenService.show(saved.id());
    return saved;
  }

  public PublicContentItem changeStatus(String id, PublicContentStatus newStatus) {
    return internalService.changeStatus(id, newStatus);
  }

  public void hide(String itemId) {
    hiddenService.hide(itemId);
  }

  public void show(String itemId) {
    hiddenService.show(itemId);
  }

  /**
   * Force refresh: triggers a live RSS fetch and stores fresh snapshot.
   * Fix from spec: does NOT write an empty snapshot.
   */
  public void forceRefreshExternal() {
    log.info("publiccontent: admin forced external RSS refresh");
    externalRssService.refreshExternalSnapshot();
  }
}
