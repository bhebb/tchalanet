package com.tchalanet.server.features.news.external;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.tchalanet.server.features.news.shared.LotteryNewsModels.LotteryNewsArticle;
import com.tchalanet.server.features.news.shared.LotteryNewsModels.LotteryNewsFeedSnapshot;
import com.tchalanet.server.features.news.shared.NewsStatus;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RomeNewsMapper {

  public LotteryNewsFeedSnapshot map(SyndFeed feed) {
    Instant fetchedAt = Instant.now();

    List<LotteryNewsArticle> articles = feed.getEntries().stream().map(this::mapEntry).toList();

    return new LotteryNewsFeedSnapshot(fetchedAt, articles);
  }

  private LotteryNewsArticle mapEntry(SyndEntry entry) {
    String title = entry.getTitle();
    String link = entry.getLink();
    URI url = link != null && !link.isBlank() ? URI.create(link) : null;

    Instant publishedAt =
        entry.getPublishedDate() != null
            ? entry.getPublishedDate().toInstant()
            : (entry.getUpdatedDate() != null ? entry.getUpdatedDate().toInstant() : Instant.now());

    String author = entry.getAuthor();
    if (author == null || author.isBlank()) {
      author = "Unknown";
    }

    List<String> categories =
        entry.getCategories().stream()
            .map(SyndCategory::getName)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();

    String description =
        entry.getDescription() != null && entry.getDescription().getValue() != null
            ? entry.getDescription().getValue()
            : "";

    String contentHtml = extractContentHtml(entry, description);

    URI commentsUrl =
        entry.getComments() != null && !entry.getComments().isBlank()
            ? URI.create(entry.getComments())
            : null;

    String guid = entry.getUri();
    String id;
    if (guid != null && !guid.isBlank()) {
      id = guid;
    } else {
      String base = link != null && !link.isBlank() ? link : title;
      id = UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }

    return new LotteryNewsArticle(
        id,
        "lotterydaily",
        title,
        url,
        commentsUrl,
        author,
        publishedAt,
        categories,
        description,
        contentHtml,
        NewsStatus.PUBLISHED);
  }

  private String extractContentHtml(SyndEntry entry, String fallbackDescription) {
    if (entry.getContents() != null && !entry.getContents().isEmpty()) {
      return entry.getContents().stream()
          .map(SyndContent::getValue)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(fallbackDescription);
    }

    if (fallbackDescription != null) {
      return fallbackDescription;
    }

    return "";
  }
}
