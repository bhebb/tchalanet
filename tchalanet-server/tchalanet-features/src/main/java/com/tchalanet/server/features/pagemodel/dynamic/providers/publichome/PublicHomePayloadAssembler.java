package com.tchalanet.server.features.pagemodel.dynamic.providers.publichome;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.common.context.TchRequestContext;
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

  private List<Map<String, Object>> buildNews(int requestedLimit) {
    int limit = requestedLimit <= 0 ? DEFAULT_NEWS_LIMIT
        : Math.min(requestedLimit, MAX_NEWS_LIMIT);
    try {
      return publicContentApi.listPublicHomeNews(limit).stream()
          .map(PublicHomePayloadAssembler::toContentMap)
          .toList();
    } catch (RuntimeException e) {
      log.warn("public_home: could not load news — {}", e.getMessage());
      return List.of();
    }
  }

  private List<Map<String, Object>> buildPlans() {
    return planCatalog.listActive().stream()
        .map(PublicHomePayloadAssembler::toPlanMap)
        .toList();
  }

  static Map<String, Object> toContentMap(PublicContentItemView item) {
    return Map.of(
        "id",          item.id() != null ? item.id().toString() : "",
        "title",       item.title() != null ? item.title() : "",
        "snippet",     item.content() != null ? item.content() : "",
        "link",        item.sourceUrl() != null ? item.sourceUrl() : "",
        "source",      item.sourceType() != null ? item.sourceType().name() : "",
        "publishedAt", item.publishedAt() != null ? item.publishedAt().toString() : "");
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
