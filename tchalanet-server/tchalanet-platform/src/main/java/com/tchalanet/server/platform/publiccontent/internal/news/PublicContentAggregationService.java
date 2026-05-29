package com.tchalanet.server.platform.publiccontent.internal.news;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSurface;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Aggregates internal and external (RSS) content for a given surface.
 *
 * <p>Surface filtering:
 * <ul>
 *   <li>Internal items: must be published and target the requested surface (empty = all).</li>
 *   <li>External RSS items: visible on all surfaces unless hidden.</li>
 *   <li>Hidden overlay excludes items regardless of type.</li>
 *   <li>Internal items are ordered first, then external, both descending by publishedAt.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicContentAggregationService {

  private final InternalPublicContentService internalService;
  private final ExternalRssNewsService externalService;
  private final HiddenPublicContentService hiddenService;

  /**
   * Aggregate published content visible on the given surface.
   * Pass {@code null} for surface to get all content (admin view).
   */
  public List<PublicContentItem> aggregateForSurface(PublicContentSurface surface, Instant now) {
    Set<String> hiddenIds = Set.copyOf(hiddenService.getHiddenIds());
    Predicate<PublicContentItem> notHidden = item -> !hiddenIds.contains(item.id());

    // Internal: published + surface match + not hidden
    var internal = internalService.findPublished(now).stream()
        .filter(item -> surface == null || item.visibleOn(surface))
        .filter(notHidden)
        .sorted(Comparator.comparing(PublicContentItem::publishedAt).reversed())
        .toList();

    // External RSS: all cached items + not hidden
    var external = externalService.getExternalSnapshot().stream()
        .filter(notHidden)
        .sorted(Comparator.comparing(PublicContentItem::publishedAt).reversed())
        .toList();

    var result = new ArrayList<PublicContentItem>(internal.size() + external.size());
    result.addAll(internal);
    result.addAll(external);
    return result;
  }
}
