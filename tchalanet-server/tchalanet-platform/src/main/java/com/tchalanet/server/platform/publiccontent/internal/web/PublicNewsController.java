package com.tchalanet.server.platform.publiccontent.internal.web;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSurface;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/news")
@RequiredArgsConstructor
@Tag(name = "Public • News")
public class PublicNewsController {

  private static final int DEFAULT_LIMIT = 20;

  private final PublicContentQueryService queryService;

  @Operation(summary = "List public news articles")
  @GetMapping
  public List<PublicContentItemView> listPublicNews(
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "PUBLIC_HOME") PublicContentSurface surface) {
    return queryService.listForSurface(surface, limit)
        .stream()
        .map(item -> new PublicContentItemView(
            toUuid(item.id()),
            item.title(),
            item.content(),
            item.imageUrl(),
            item.sourceUrl() != null ? item.sourceUrl().toString() : null,
            item.sourceType(),
            item.publishedAt()))
        .toList();
  }

  private static UUID toUuid(String id) {
    if (id == null) return null;
    try { return UUID.fromString(id); }
    catch (IllegalArgumentException e) { return UUID.nameUUIDFromBytes(id.getBytes()); }
  }
}
