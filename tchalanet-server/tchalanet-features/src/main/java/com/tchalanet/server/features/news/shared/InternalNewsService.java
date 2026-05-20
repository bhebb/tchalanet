package com.tchalanet.server.features.news.shared;

import com.tchalanet.server.common.cache.CacheKeyBuilder;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalNewsService {

  private final NewsCache newsCache;
  private final CacheKeyBuilder cacheKeyBuilder;

  /** Liste toutes les news internes (pour l'écran admin). */
  public LotteryNewsModels.LotteryNewsFeedSnapshot findAll() {
    return newsCache.getLatestNews(cacheKeyBuilder.newsInternalKey());
  }

  /**
   * News internes visibles publiquement à l’instant donné : status = PUBLISHED, dans la fenêtre
   * [publishedAt, expiresAt].
   */
  public List<LotteryNewsModels.LotteryNewsArticle> findPublished(Instant now) {
    return findAll().articles().stream()
        .filter(n -> n.status() == NewsStatus.PUBLISHED)
        .filter(n -> !n.publishedAt().isAfter(now))
        .toList();
  }

  /**
   * Crée ou met à jour une news interne (mapping UI contract → InternalNewsItem à faire dans
   * AdminNewsService).
   */
  public LotteryNewsModels.LotteryNewsArticle save(LotteryNewsModels.LotteryNewsArticle item) {
    newsCache.putLatestNews(
        cacheKeyBuilder.newsInternalKey(),
        LotteryNewsModels.LotteryNewsFeedSnapshot.publishNow(item));
    return findAll().articles().stream()
        .filter(t -> t.title().equals(item.title()))
        .findFirst()
        .orElse(item);
  }

  public Optional<LotteryNewsModels.LotteryNewsArticle> findById(UUID id) {
    return findAll().articles().stream().filter(t -> t.id().equals(id.toString())).findFirst();
  }

  /**
   * Change uniquement le status (DRAFT/PUBLISHED/ARCHIVED). La logique d'evict cache reste dans
   * AdminNewsService.
   */
  public LotteryNewsModels.LotteryNewsArticle changeStatus(UUID id, NewsStatus newStatus) {
    var existing =
        findById(id).orElseThrow(() -> new NoSuchElementException("News not found: " + id));

    var updated = existing.withStatus(newStatus);
    return save(updated);
  }
}
