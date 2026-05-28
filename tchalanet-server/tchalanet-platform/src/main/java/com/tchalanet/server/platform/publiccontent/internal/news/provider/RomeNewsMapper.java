package com.tchalanet.server.platform.publiccontent.internal.news.provider;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSourceType;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentStatus;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentItem;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RomeNewsMapper {

  public List<PublicContentItem> map(SyndFeed feed) {
    return feed.getEntries().stream().map(this::mapEntry).toList();
  }

  private PublicContentItem mapEntry(SyndEntry entry) {
    String title = entry.getTitle();
    String link = entry.getLink();
    URI url = link != null && !link.isBlank() ? URI.create(link) : null;

    Instant publishedAt =
        entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant()
        : entry.getUpdatedDate() != null ? entry.getUpdatedDate().toInstant()
        : Instant.now();

    String author = entry.getAuthor();
    if (author == null || author.isBlank()) author = "Unknown";

    List<String> categories = entry.getCategories().stream()
        .map(SyndCategory::getName).filter(Objects::nonNull)
        .map(String::trim).filter(s -> !s.isBlank()).toList();

    String description = entry.getDescription() != null
        ? entry.getDescription().getValue() : "";

    String contentHtml = extractContentHtml(entry, description);

    URI commentsUrl = entry.getComments() != null && !entry.getComments().isBlank()
        ? URI.create(entry.getComments()) : null;

    String guid = entry.getUri();
    String id;
    if (guid != null && !guid.isBlank()) {
      id = guid;
    } else {
      String base = link != null && !link.isBlank() ? link : (title != null ? title : "");
      id = UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }

    return new PublicContentItem(
        id, "lotterydaily", PublicContentSourceType.EXTERNAL_RSS,
        title, description, contentHtml, null,
        url, author,
        PublicContentStatus.PUBLISHED,
        Set.of(),         // external RSS = visible on all surfaces
        publishedAt, null,
        categories);
  }

  private String extractContentHtml(SyndEntry entry, String fallback) {
    if (entry.getContents() != null && !entry.getContents().isEmpty()) {
      return entry.getContents().stream()
          .map(SyndContent::getValue).filter(Objects::nonNull)
          .findFirst().orElse(fallback);
    }
    return fallback != null ? fallback : "";
  }
}
