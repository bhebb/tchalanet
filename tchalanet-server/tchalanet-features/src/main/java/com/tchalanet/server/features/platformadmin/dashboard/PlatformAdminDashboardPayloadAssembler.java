package com.tchalanet.server.features.platformadmin.dashboard;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatsView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView;
import com.tchalanet.server.core.analytics.api.query.GetPlatformDashboardStatsQuery;
import com.tchalanet.server.core.subscription.api.query.GetPlatformSubscriptionStatsQuery;
import com.tchalanet.server.core.subscription.api.query.PlatformSubscriptionStatsView;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code platform_admin_dashboard}.
 * One assembly per request — memoized via {@code PageModelResolutionContext}.
 *
 * Grouped reads (target ≤ 5 per dashboard-overview-runtime-v1 §12):
 *   1. HealthEndpoint.health()                                     (in-process, no SQL)
 *   2. TenantCatalog.stats()                                       (tenant counts)
 *   3. QueryBus.ask(GetPlatformDashboardStatsQuery)                (analytics KPIs)
 *   4. QueryBus.ask(GetPlatformSubscriptionStatsQuery)             (subscriptions)
 *   5. TenantCatalog.listTenants(size=50) + PublicContentApi       (onboarding + content)
 *
 * Platform alerts placeholder: no cross-platform alert aggregate yet (TODO V2).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminDashboardPayloadAssembler {

  private static final int PUBLIC_CONTENT_LIMIT = 5;

  private final TenantCatalog tenantCatalog;
  private final QueryBus queryBus;
  private final PublicContentApi publicContentApi;
  /** Optional — adapter typically lives in the app layer (wraps actuator HealthEndpoint). */
  private final ObjectProvider<PlatformHealthProbe> healthProbeProvider;

  public Payload assemble(TchRequestContext ctx) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    Map<String, Object> health          = buildHealth();
    Map<String, Object> tenants         = buildTenantsKpi(today);
    Map<String, Object> subscriptions   = buildSubscriptions();
    Map<String, Object> onboardingAlerts = buildOnboardingAlerts();
    Map<String, Object> platformAlerts  = buildPlatformAlerts();
    Map<String, Object> publicContent   = buildPublicContent();
    Map<String, Object> quickActions    = buildQuickActions();

    return new Payload(health, tenants, subscriptions, onboardingAlerts,
        platformAlerts, publicContent, quickActions);
  }

  /**
   * Platform health snapshot from Spring Boot's {@link org.springframework.boot.actuate.health.HealthEndpoint}.
   * No SQL — reads the in-process health registry the actuator already maintains.
   */
  private Map<String, Object> buildHealth() {
    PlatformHealthProbe probe = healthProbeProvider.getIfAvailable();
    if (probe == null) {
      return Map.of("global", "UNKNOWN", "components", Map.of());
    }
    try {
      Map<String, Object> snapshot = probe.snapshot();
      return snapshot != null ? snapshot : Map.of("global", "UNKNOWN", "components", Map.of());
    } catch (RuntimeException e) {
      return Map.of("global", "UNKNOWN", "components", Map.of());
    }
  }

  /**
   * Tenant counts (from catalog) + analytics KPIs (tickets/sales today, top tenants).
   * Two grouped reads bundled in one widget section.
   */
  private Map<String, Object> buildTenantsKpi(LocalDate today) {
    long total = 0L;
    long active = 0L;
    long suspended = 0L;
    long onboarding = 0L;

    try {
      TenantStatsView catalogStats = tenantCatalog.stats();
      if (catalogStats != null) {
        total = catalogStats.total();
        active = catalogStats.active();
        suspended = catalogStats.suspended();
      }
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: tenant catalog stats unavailable — {}", e.getMessage());
    }

    long ticketsSoldToday   = 0L;
    BigDecimal grossSalesToday = BigDecimal.ZERO;
    BigDecimal netToday        = BigDecimal.ZERO;
    List<Map<String, Object>> topTenants = List.of();

    try {
      PlatformDashboardStatsView statsView =
          queryBus.ask(GetPlatformDashboardStatsQuery.today(today));
      if (statsView != null) {
        if (statsView.summary() != null) {
          // Use analytics totalTenants as fallback if catalog returned zero
          if (total == 0L) total = statsView.summary().totalTenants();
          ticketsSoldToday = statsView.summary().ticketsSold();
          grossSalesToday  = statsView.summary().grossSales() != null
              ? statsView.summary().grossSales() : BigDecimal.ZERO;
          netToday         = statsView.summary().netRevenueEstimated() != null
              ? statsView.summary().netRevenueEstimated() : BigDecimal.ZERO;
        }
        if (statsView.topTenants() != null) {
          topTenants = statsView.topTenants().stream()
              .map(r -> Map.<String, Object>of(
                  "tenantCode",    r.tenantCode() != null ? r.tenantCode() : "",
                  "ticketsSold",   r.ticketsSold(),
                  "grossSales",    r.grossSales() != null ? r.grossSales() : BigDecimal.ZERO,
                  "netRevenue",    r.netRevenueEstimated() != null ? r.netRevenueEstimated() : BigDecimal.ZERO))
              .toList();
        }
      }
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: analytics KPIs unavailable — {}", e.getMessage());
    }

    return Map.ofEntries(
        Map.entry("total",            total),
        Map.entry("active",           active),
        Map.entry("suspended",        suspended),
        Map.entry("onboarding",       onboarding),
        Map.entry("actionRequired",   suspended),
        Map.entry("ticketsSoldToday", ticketsSoldToday),
        Map.entry("grossSalesToday",  grossSalesToday),
        Map.entry("netToday",         netToday),
        Map.entry("topTenants",       topTenants));
  }

  private Map<String, Object> buildSubscriptions() {
    try {
      PlatformSubscriptionStatsView view = queryBus.ask(new GetPlatformSubscriptionStatsQuery());
      if (view != null) {
        return Map.of(
            "active",  (long) view.active(),
            "trial",   0L,
            "pastDue", (long) view.pastDue(),
            "expired", (long) view.canceled(),
            "total",   (long) view.total(),
            "byPlan",  view.byPlan() != null ? view.byPlan() : List.of());
      }
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: subscription stats unavailable — {}", e.getMessage());
    }
    return Map.of("active", 0L, "trial", 0L, "pastDue", 0L, "expired", 0L,
        "total", 0L, "byPlan", List.of());
  }

  /**
   * Tenants currently in DRAFT (onboarding in progress) — up to 10.
   * Filtered in-memory; catalog status filter is a V2 addition.
   */
  private Map<String, Object> buildOnboardingAlerts() {
    try {
      TchPage<TenantRegistryView> page = tenantCatalog.listTenants(PageRequest.of(0, 50));
      if (page != null && page.items() != null) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (TenantRegistryView t : page.items()) {
          if (t.status() == TenantStatus.DRAFT && items.size() < 10) {
            items.add(Map.of(
                "tenantId", t.tenantId().value().toString(),
                "code",     t.code(),
                "name",     t.name() != null ? t.name() : "",
                "status",   t.status().name()));
          }
        }
        return Map.of("items", items, "count", items.size());
      }
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: onboarding alerts unavailable — {}", e.getMessage());
    }
    return Map.of("items", List.of(), "count", 0);
  }

  /** V1 placeholder: no cross-platform alerts aggregate yet (TODO V2). */
  private Map<String, Object> buildPlatformAlerts() {
    return Map.of("items", List.of(), "count", 0);
  }

  private Map<String, Object> buildPublicContent() {
    try {
      List<PublicContentItemView> items =
          publicContentApi.listPlatformAdminDashboardNews(PUBLIC_CONTENT_LIMIT);
      return Map.of(
          "items", items.stream()
              .map(i -> Map.<String, Object>of(
                  "id",          i.id() != null ? i.id().toString() : "",
                  "title",       i.title() != null ? i.title() : "",
                  "snippet",     i.content() != null ? i.content() : "",
                  "link",        i.sourceUrl() != null ? i.sourceUrl() : "",
                  "publishedAt", i.publishedAt() != null ? i.publishedAt().toString() : ""))
              .toList(),
          "count", items.size());
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: public content unavailable — {}", e.getMessage());
      return Map.of("items", List.of(), "count", 0);
    }
  }

  private Map<String, Object> buildQuickActions() {
    return Map.of(
        "actions", List.of(
            Map.of("id", "CREATE_TENANT",     "label", "Créer un tenant",       "route", "/app/platform/tenants/new"),
            Map.of("id", "PLATFORM_OVERVIEW", "label", "Aperçu plateforme",     "route", "/app/platform/overview"),
            Map.of("id", "OPS_HEALTH",        "label", "Santé plateforme",      "route", "/app/platform/ops/health")));
  }

  public record Payload(
      Map<String, Object> health,
      Map<String, Object> tenants,
      Map<String, Object> subscriptions,
      Map<String, Object> onboardingAlerts,
      Map<String, Object> platformAlerts,
      Map<String, Object> publicContent,
      Map<String, Object> quickActions) {}
}
