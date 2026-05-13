package com.tchalanet.server.features.news.admin;

import com.tchalanet.server.common.cache.internal.CacheKeyBuilder;
import com.tchalanet.server.features.news.admin.model.AdminNewsItem;
import com.tchalanet.server.features.news.admin.model.AdminNewsListResponse;
import com.tchalanet.server.features.news.admin.model.AdminUpsertNewsRequest;
import com.tchalanet.server.features.news.shared.LotteryNewsModels;
import com.tchalanet.server.features.news.shared.NewsStatus;
import com.tchalanet.server.features.news.shared.NewsCache;
import com.tchalanet.server.features.news.shared.ExternalNewsService;
import com.tchalanet.server.features.news.shared.HiddenNewsService;
import com.tchalanet.server.features.news.shared.InternalNewsService;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNewsService {

  private static final String INTERNAL_SOURCE_ID = "internal"; // à toi de choisir le label

  private final InternalNewsService internalNewsService;
  private final ExternalNewsService externalNewsService;
  private final HiddenNewsService hiddenNewsService;
  private final NewsCache newsCache;
  private final CacheKeyBuilder cacheKeyBuilder;

  /** 1) Liste admin: internes + externes + flag hidden. */
  public AdminNewsListResponse list() {
    var hiddenIds = hiddenNewsService.getHiddenIds();

    // internes: on prend tout le snapshot interne
    var internalSnapshot = internalNewsService.findAll();
    var internalItems =
        internalSnapshot.articles().stream()
            .sorted(
                Comparator.comparing(LotteryNewsModels.LotteryNewsArticle::publishedAt).reversed())
            .map(a -> toAdminItem(a, hiddenIds.contains(a.id())))
            .toList();

    // externes: on prend le snapshot provider (ou cache)
    var externalSnapshot = externalNewsService.fetchSnapshot();
    var externalItems =
        externalSnapshot.articles().stream()
            .sorted(
                Comparator.comparing(LotteryNewsModels.LotteryNewsArticle::publishedAt).reversed())
            .map(a -> toAdminItem(a, hiddenIds.contains(a.id())))
            .toList();

    return new AdminNewsListResponse(internalItems, externalItems);
  }

  /** 2) Upsert d'une news interne (création / update). */
  public AdminNewsItem upsert(AdminUpsertNewsRequest request) {
    var now = Instant.now();

    LotteryNewsModels.LotteryNewsArticle article;
    if (request.id() != null) {
      // update: retrouver et modifier
      var existingOpt = internalNewsService.findById(request.id());
      if (existingOpt.isPresent()) {
        var existing = existingOpt.get();
        article =
            new LotteryNewsModels.LotteryNewsArticle(
                existing.id(),
                INTERNAL_SOURCE_ID,
                request.title(),
                existing.url(), // pas d'URL publique interne V1 -> null possible
                existing.commentsUrl(),
                existing.author(),
                request.publishedAt() != null ? request.publishedAt() : existing.publishedAt(),
                request.categories() != null ? request.categories() : existing.categories(),
                request.description(),
                request.contentHtml(),
                request.status() != null ? request.status() : existing.status());
      } else {
        // si l'id n'existe pas, on bascule en création
        article = buildInternalArticleForCreate(request, now);
      }
    } else {
      // création
      article = buildInternalArticleForCreate(request, now);
    }

    var saved = internalNewsService.save(article);
    // On considère qu'un upsert interne réinitialise le flag hidden -> pas hidden
    hiddenNewsService.show(saved.id());

    return toAdminItem(saved, false);
  }

  private LotteryNewsModels.LotteryNewsArticle buildInternalArticleForCreate(
      AdminUpsertNewsRequest request, Instant now) {
    String id = UUID.randomUUID().toString();
    return new LotteryNewsModels.LotteryNewsArticle(
        id,
        INTERNAL_SOURCE_ID,
        request.title(),
        (URI) null, // pas d'URL spécifique interne pour l'instant
        null,
        "Tchalanet", // auteur V1 (tu peux mettre l'user courant plus tard)
        request.publishedAt() != null ? request.publishedAt() : now,
        request.categories() != null ? request.categories() : List.of(),
        request.description(),
        request.contentHtml(),
        request.status() != null ? request.status() : NewsStatus.DRAFT);
  }

  /** 3) Changer le statut d'une news interne. */
  public AdminNewsItem changeStatus(UUID id, NewsStatus newStatus) {
    var updated = internalNewsService.changeStatus(id, newStatus);
    // si on la repasse en PUBLISHED, on laisse le hidden tel quel (admin peut gérer hide/show)
    var hiddenIds = hiddenNewsService.getHiddenIds();
    boolean hidden = hiddenIds.contains(updated.id());
    return toAdminItem(updated, hidden);
  }

  /** 4) Cacher un article (interne ou externe) via son id. */
  public void hide(String articleId) {
    hiddenNewsService.hide(articleId);
    log.info("News {} hidden by admin", articleId);
  }

  /** 5) Réafficher un article (interne ou externe). */
  public void show(String articleId) {
    hiddenNewsService.show(articleId);
    log.info("News {} shown by admin (removed from hidden list)", articleId);
  }

  /**
   * 6) Force refresh: - rafraîchit le snapshot externe - invalide le cache agrégé public (news
   * publics)
   */
  public void forceRefresh() {
    // 1) refresh feed externe depuis le provider (et remettre en cache externe)
    externalNewsService.fetchSnapshot();

    // 2) invalider le cache agrégé public (V1: on met un snapshot vide; la prochaine
    //    requête publique va reconstituer à partir d'external + internal)
    newsCache.putLatestNews(
        cacheKeyBuilder.newsExternalKey(), LotteryNewsModels.LotteryNewsFeedSnapshot.empty());

    log.info("Admin forced news refresh (external feed + public snapshot)");
  }

  // --- mapping ---

  private AdminNewsItem toAdminItem(LotteryNewsModels.LotteryNewsArticle a, boolean hidden) {
    return new AdminNewsItem(
        a.id(), a.sourceId(), a.title(), a.description(), a.status(), hidden, a.publishedAt());
  }
}
