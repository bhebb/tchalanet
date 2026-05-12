package com.tchalanet.server.features.news.shared;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.lang.Nullable;

/** Modèles forts pour représenter un snapshot de flux de news loterie et ses articles. */
public final class LotteryNewsModels {

  private LotteryNewsModels() {}

  /** Snapshot complet mis en cache (cacheName = "news"). */
  public record LotteryNewsFeedSnapshot(Instant fetchedAt, List<LotteryNewsArticle> articles) {
    public static LotteryNewsFeedSnapshot empty() {
      return new LotteryNewsFeedSnapshot(Instant.now(), List.of());
    }

    public static LotteryNewsFeedSnapshot publishNow(LotteryNewsArticle item) {
      return new LotteryNewsFeedSnapshot(Instant.now(), List.of(item));
    }
  }

  public record LotteryNewsArticle(
      String id, // guid ou hash du link
      String sourceId, // "lotterydaily"
      String title, // <title>
      URI url, // <link>
      @Nullable URI commentsUrl, // <comments>
      String author, // <dc:creator>
      Instant publishedAt, // <pubDate>
      List<String> categories, // <category>...
      String description, // <description> (résumé)
      String contentHtml, // <content:encoded> complet
      NewsStatus status // statut éditorial local (ex: PUBLISHED)
      ) {

    public LotteryNewsArticle withStatus(NewsStatus newStatus) {
      return new LotteryNewsArticle(
          id,
          sourceId,
          title,
          url,
          commentsUrl,
          author,
          publishedAt,
          categories,
          description,
          contentHtml,
          newStatus);
    }
  }
}
