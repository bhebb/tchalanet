package com.tchalanet.server.features.pagemodel.dynamic.providers.publichome;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.common.context.TchRequestContext;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code public_home}.
 * One assembly per request — sub-payloads are extracted by widgetId in
 * {@link PublicHomeProvider}.
 */
@Component
@RequiredArgsConstructor
public class PublicHomePayloadAssembler {

  private final PlanCatalog planCatalog;

  public Payload assemble(String lang, TchRequestContext ctx) {
    boolean hasTenant = ctx != null && ctx.tenantId() != null;
    return new Payload(
        buildHero(lang, hasTenant, ctx),
        buildFeatures(lang),
        buildPlans()
    );
  }

  private Map<String, Object> buildHero(String lang, boolean hasTenant, TchRequestContext ctx) {
    return Map.of(
        "tagline", resolveTagline(lang),
        "ctaLinks", buildCtaLinks(lang, hasTenant),
        "stats", hasTenant ? Map.of("tenantCode",
            ctx.effectiveTenantCode() != null ? ctx.effectiveTenantCode() : "") : Map.of(),
        "backgroundUrl", "");
  }

  private List<Map<String, Object>> buildFeatures(String lang) {
    // V1: feature grid is rendered from PageModel props (items_key) — provider returns
    // metadata only. Replace with catalog-driven feature list once available.
    return List.of();
  }

  private List<Map<String, Object>> buildPlans() {
    return planCatalog.listActive().stream()
        .map(PublicHomePayloadAssembler::toPlanMap)
        .toList();
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

  private static String resolveTagline(String lang) {
    if ("ht".equals(lang)) return "Jwè ak konfyans";
    if ("en".equals(lang)) return "Play with confidence";
    return "Jouez en toute confiance";
  }

  private static List<Map<String, Object>> buildCtaLinks(String lang, boolean hasTenant) {
    if (hasTenant) {
      return List.of(
          Map.of("label", label("dashboard", lang), "href", "/dashboard"),
          Map.of("label", label("tickets", lang), "href", "/tickets"));
    }
    return List.of(
        Map.of("label", label("register", lang), "href", "/register"),
        Map.of("label", label("learn_more", lang), "href", "/about"));
  }

  private static String label(String key, String lang) {
    return switch (key + ":" + (lang != null ? lang : "fr")) {
      case "dashboard:ht" -> "Tablo de bò";
      case "dashboard:en" -> "Dashboard";
      case "tickets:ht" -> "Tikè";
      case "tickets:en" -> "Tickets";
      case "register:ht" -> "Enskri";
      case "register:en" -> "Register";
      case "learn_more:ht" -> "Aprann plis";
      case "learn_more:en" -> "Learn more";
      default -> key;
    };
  }

  public record Payload(
      Map<String, Object> hero,
      List<Map<String, Object>> features,
      List<Map<String, Object>> plans) {}
}
