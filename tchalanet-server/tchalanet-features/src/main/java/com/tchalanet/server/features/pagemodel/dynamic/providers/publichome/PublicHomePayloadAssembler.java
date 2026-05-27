package com.tchalanet.server.features.pagemodel.dynamic.providers.publichome;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.news.publicnews.PublicNewsService;
import com.tchalanet.server.features.news.shared.LotteryNewsModels.LotteryNewsArticle;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code public_home}.
 * One assembly per request — sub-payloads are extracted by widgetId in
 * {@link PublicHomeProvider}.
 *
 * V1 (révision 2) ne traite plus que les widgets dynamiques :
 *   - home.news  : ticker des dernières news via {@link PublicNewsService}
 *   - home.plans : grille des plans actifs via {@link PlanCatalog}
 *
 * Hero, features et tchala sont sortis du provider — ils passent désormais
 * par {@code json_file} (fragments {@code public_hero}, {@code public_features},
 * {@code public_tchala}).
 */
@Component
@RequiredArgsConstructor
public class PublicHomePayloadAssembler {

  static final int DEFAULT_NEWS_LIMIT = 5;
  static final int MAX_NEWS_LIMIT = 20;

  private final PublicNewsService newsService;
  private final PlanCatalog planCatalog;

  public Payload assemble(int newsLimit, TchRequestContext ctx) {
    return new Payload(buildNews(newsLimit), buildPlans());
  }

  private List<Map<String, Object>> buildNews(int requestedLimit) {
    int limit = requestedLimit <= 0 ? DEFAULT_NEWS_LIMIT
        : Math.min(requestedLimit, MAX_NEWS_LIMIT);
    return newsService.listAll().stream()
        .limit(limit)
        .map(PublicHomePayloadAssembler::toNewsMap)
        .toList();
  }

  private List<Map<String, Object>> buildPlans() {
    return planCatalog.listActive().stream()
        .map(PublicHomePayloadAssembler::toPlanMap)
        .toList();
  }

  private static Map<String, Object> toNewsMap(LotteryNewsArticle article) {
    return Map.of(
        "id", article.id() != null ? article.id() : "",
        "title", article.title() != null ? article.title() : "",
        "snippet", article.description() != null ? article.description() : "",
        "link", article.url() != null ? article.url().toString() : "",
        "source", article.sourceId() != null ? article.sourceId() : "",
        "publishedAt", article.publishedAt() != null ? article.publishedAt().toString() : "");
  }

  private static Map<String, Object> toPlanMap(PlanView plan) {
    return Map.of(
        "value", plan.code() != null ? plan.code() : "",
        "name", plan.name() != null ? plan.name() : "",
        "description", plan.description() != null ? plan.description() : "",
        "price", plan.priceAmount() != null ? plan.priceAmount() : 0,
        "currency", plan.currency() != null ? plan.currency() : "",
        "billingPeriod", plan.billingPeriod() != null ? plan.billingPeriod() : "",
        "features", plan.featuresJson() != null ? plan.featuresJson() : Map.of(),
        "isDefault", plan.isDefault());
  }

  public record Payload(
      List<Map<String, Object>> news,
      List<Map<String, Object>> plans) {}
}
