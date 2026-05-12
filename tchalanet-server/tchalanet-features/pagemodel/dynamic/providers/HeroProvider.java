package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * E.3 — Provider du widget hero (bannière principale).
 * Source : "hero"
 * Léger — pas de DB query lourde, principalement données enrichies contextuelles.
 */
@Component
public class HeroProvider implements PageModelDynamicProvider {

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "hero".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {

    // Enrichissement contextuel tenant si disponible
    boolean hasTenant = ctx != null && ctx.tenantId() != null;

    Map<String, Object> payload =
        Map.of(
            "tagline", resolveTagline(lang),
            "ctaLinks", buildCtaLinks(lang, hasTenant),
            "stats", hasTenant ? buildTenantStats(ctx) : Map.of(),
            "backgroundUrl", "");

    return payload;
  }

  private String resolveTagline(String lang) {
    if ("ht".equals(lang)) return "Jwè ak konfyans";
    if ("en".equals(lang)) return "Play with confidence";
    return "Jouez en toute confiance"; // fr par défaut
  }

  private List<Map<String, Object>> buildCtaLinks(String lang, boolean hasTenant) {
    if (hasTenant) {
      return List.of(
          Map.of("label", resolveLabel("dashboard", lang), "href", "/dashboard"),
          Map.of("label", resolveLabel("tickets", lang), "href", "/tickets"));
    }
    return List.of(
        Map.of("label", resolveLabel("register", lang), "href", "/register"),
        Map.of("label", resolveLabel("learn_more", lang), "href", "/about"));
  }

  private Map<String, Object> buildTenantStats(TchRequestContext ctx) {
    // Stats légères — enrichissement futur possible via QueryBus
    return Map.of(
        "tenantCode", ctx.effectiveTenantCode() != null ? ctx.effectiveTenantCode() : "");
  }

  private String resolveLabel(String key, String lang) {
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

  @Override
  public String providerKey() {
    return "hero";
  }
}

