package com.tchalanet.server.features.pagemodel.dynamic.providers.publichome;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.pagemodel.contract.NewsItem;
import com.tchalanet.server.features.pagemodel.contract.PlanItem;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code public_home}.
 * One assembly per request — sub-payloads are extracted by widgetId in
 * {@link PublicHomeProvider}.
 *
 * V1 (révision 2) widgets dynamiques :
 *   - home.news  : actualités publiques via {@link PublicContentApi}
 *   - home.plans : grille des plans actifs via {@link PlanCatalog}
 *
 * Hero, features et tchala passent par {@code json_file}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicHomePayloadAssembler {

  static final int DEFAULT_NEWS_LIMIT = 5;
  static final int MAX_NEWS_LIMIT = 20;

  private final PublicContentApi publicContentApi;
  private final PlanCatalog planCatalog;

  public Payload assemble(int newsLimit, TchRequestContext ctx) {
    return new Payload(buildNews(newsLimit), buildPlans());
  }

  private List<NewsItem> buildNews(int requestedLimit) {
    int limit = requestedLimit <= 0 ? DEFAULT_NEWS_LIMIT
        : Math.min(requestedLimit, MAX_NEWS_LIMIT);
    try {
      return publicContentApi.listPublicHomeNews(limit).stream()
          .map(PublicHomePayloadAssembler::toNewsItem)
          .toList();
    } catch (RuntimeException e) {
      log.warn("public_home: could not load news — {}", e.getMessage());
      return List.of();
    }
  }

  private List<PlanItem> buildPlans() {
    return planCatalog.listActive().stream()
        .map(PublicHomePayloadAssembler::toPlanItem)
        .toList();
  }

  static NewsItem toNewsItem(PublicContentItemView item) {
    return new NewsItem(
        item.id() != null ? item.id().toString() : "",
        item.title() != null ? item.title() : "",
        item.content() != null ? item.content() : "",
        item.sourceUrl() != null ? item.sourceUrl() : "",
        item.sourceType() != null ? item.sourceType().name() : "",
        item.publishedAt() != null ? item.publishedAt().toString() : "");
  }

  @SuppressWarnings("unchecked")
  private static PlanItem toPlanItem(PlanView plan) {
    Object featuresRaw = plan.featuresJson() != null ? plan.featuresJson() : Map.of();
    Map<String, Object> features = featuresRaw instanceof Map<?, ?> m
        ? (Map<String, Object>) m : Map.of();
    return new PlanItem(
        plan.code() != null ? plan.code() : "",
        plan.name() != null ? plan.name() : "",
        plan.description() != null ? plan.description() : "",
        plan.priceAmount() != null ? plan.priceAmount() : 0,
        plan.currency() != null ? plan.currency() : "",
        plan.billingPeriod() != null ? plan.billingPeriod() : "",
        features,
        plan.isDefault());
  }

  public record Payload(
      List<NewsItem> news,
      List<PlanItem> plans) {}
}
