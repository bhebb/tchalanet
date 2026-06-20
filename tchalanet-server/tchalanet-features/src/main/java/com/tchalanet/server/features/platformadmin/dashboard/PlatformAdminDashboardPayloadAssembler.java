package com.tchalanet.server.features.platformadmin.dashboard;

import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatsView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView;
import com.tchalanet.server.core.analytics.api.query.GetPlatformDashboardStatsQuery;
import com.tchalanet.server.core.subscription.api.query.GetPlatformSubscriptionStatsQuery;
import com.tchalanet.server.core.subscription.api.query.PlatformSubscriptionStatsView;
import com.tchalanet.server.features.pagemodel.contract.ActionItem;
import com.tchalanet.server.features.pagemodel.contract.AlertItem;
import com.tchalanet.server.features.pagemodel.contract.NewsItem;
import com.tchalanet.server.features.pagemodel.contract.PublicContentPayload;
import com.tchalanet.server.features.pagemodel.contract.QuickActionsPayload;
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

  private final TenantPreContextLookupApi tenantPreContextLookupApi;
  private final QueryBus queryBus;
  private final PublicContentApi publicContentApi;
  /** Optional — adapter typically lives in the app layer (wraps actuator HealthEndpoint). */
  private final ObjectProvider<PlatformHealthProbe> healthProbeProvider;

  public Payload assemble(TchRequestContext ctx) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    PlatformHealthPayload health              = buildHealth();
    TenantKpiSummaryPayload tenants           = buildTenantsKpi(today);
    SubscriptionSummaryPayload subscriptions  = buildSubscriptions();
    OnboardingAlertsPayload onboardingAlerts  = buildOnboardingAlerts();
    PlatformAlertsPayload platformAlerts      = buildPlatformAlerts();
    PublicContentPayload publicContent        = buildPublicContent();
    QuickActionsPayload quickActions          = buildQuickActions();

    return new Payload(health, tenants, subscriptions, onboardingAlerts,
        platformAlerts, publicContent, quickActions);
  }

  /**
   * Platform health snapshot from Spring Boot's {@link org.springframework.boot.actuate.health.}.
   * No SQL — reads the in-process health registry the actuator already maintains.
   */
  @SuppressWarnings("unchecked")
  private PlatformHealthPayload buildHealth() {
    PlatformHealthProbe probe = healthProbeProvider.getIfAvailable();
    if (probe == null) {
      return new PlatformHealthPayload("UNKNOWN", Map.of());
    }
    try {
      Map<String, Object> snapshot = probe.snapshot();
      if (snapshot == null) {
        return new PlatformHealthPayload("UNKNOWN", Map.of());
      }
      String global = snapshot.getOrDefault("global", "UNKNOWN").toString();
      Object rawComponents = snapshot.get("components");
      Map<String, String> components = Map.of();
      if (rawComponents instanceof Map<?, ?> m) {
        var typed = new java.util.LinkedHashMap<String, String>();
        m.forEach((k, v) -> typed.put(String.valueOf(k), String.valueOf(v)));
        components = java.util.Collections.unmodifiableMap(typed);
      }
      return new PlatformHealthPayload(global, components);
    } catch (RuntimeException e) {
      return new PlatformHealthPayload("UNKNOWN", Map.of());
    }
  }

  /**
   * Tenant counts (from catalog) + analytics KPIs (tickets/sales today, top tenants).
   * Two grouped reads bundled in one widget section.
   */
  private TenantKpiSummaryPayload buildTenantsKpi(LocalDate today) {
    long total = 0L;
    long active = 0L;
    long suspended = 0L;
    long onboarding = 0L;

    try {
      TenantStatsView catalogStats = tenantPreContextLookupApi.stats();
      if (catalogStats != null) {
        total = catalogStats.total();
        active = catalogStats.active();
        suspended = catalogStats.suspended();
      }
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: tenant catalog stats unavailable — {}", e.getMessage());
    }

    long ticketsSoldToday      = 0L;
    BigDecimal grossSalesToday = BigDecimal.ZERO;
    BigDecimal netToday        = BigDecimal.ZERO;
    List<TenantRankItem> topTenants = List.of();

    try {
      PlatformDashboardStatsView statsView =
          queryBus.ask(GetPlatformDashboardStatsQuery.today(today));
      if (statsView != null) {
        if (statsView.summary() != null) {
          if (total == 0L) total = statsView.summary().totalTenants();
          ticketsSoldToday = statsView.summary().ticketsSold();
          grossSalesToday  = statsView.summary().grossSales() != null
              ? statsView.summary().grossSales() : BigDecimal.ZERO;
          netToday         = statsView.summary().netRevenueEstimated() != null
              ? statsView.summary().netRevenueEstimated() : BigDecimal.ZERO;
        }
        if (statsView.topTenants() != null) {
          topTenants = statsView.topTenants().stream()
              .map(r -> new TenantRankItem(
                  r.tenantCode() != null ? r.tenantCode() : "",
                  r.ticketsSold(),
                  r.grossSales() != null ? r.grossSales() : BigDecimal.ZERO,
                  r.netRevenueEstimated() != null ? r.netRevenueEstimated() : BigDecimal.ZERO))
              .toList();
        }
      }
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: analytics KPIs unavailable — {}", e.getMessage());
    }

    return new TenantKpiSummaryPayload(
        total, active, suspended, onboarding, suspended,
        ticketsSoldToday, grossSalesToday, netToday, topTenants);
  }

  private SubscriptionSummaryPayload buildSubscriptions() {
    try {
      PlatformSubscriptionStatsView view = queryBus.ask(new GetPlatformSubscriptionStatsQuery());
      if (view != null) {
        return new SubscriptionSummaryPayload(
            view.active(), 0L, view.pastDue(), view.canceled(), view.total(),
            view.byPlan() != null ? view.byPlan() : List.of());
      }
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: subscription stats unavailable — {}", e.getMessage());
    }
    return SubscriptionSummaryPayload.empty();
  }

  /**
   * Tenants currently in DRAFT (onboarding in progress) — up to 10.
   * Filtered in-memory; catalog status filter is a V2 addition.
   */
  private OnboardingAlertsPayload buildOnboardingAlerts() {
    try {
      TchPage<TenantContextLookupView> page = tenantPreContextLookupApi.listTenants(PageRequest.of(0, 50));
      if (page != null && page.items() != null) {
        List<OnboardingAlertItem> items = new ArrayList<>();
        for (TenantContextLookupView t : page.items()) {
          if (t.status() == TenantStatus.DRAFT && items.size() < 10) {
            items.add(new OnboardingAlertItem(
                t.tenantId().value().toString(),
                t.code() != null ? t.code() : "",
                t.name() != null ? t.name() : "",
                t.status().name()));
          }
        }
        return new OnboardingAlertsPayload(List.copyOf(items), items.size());
      }
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: onboarding alerts unavailable — {}", e.getMessage());
    }
    return OnboardingAlertsPayload.empty();
  }

  /** V1 placeholder: no cross-platform alerts aggregate yet (TODO V2). */
  private PlatformAlertsPayload buildPlatformAlerts() {
    return PlatformAlertsPayload.empty();
  }

  private PublicContentPayload buildPublicContent() {
    try {
      List<PublicContentItemView> items =
          publicContentApi.listPlatformAdminDashboardNews(PUBLIC_CONTENT_LIMIT);
      List<NewsItem> news = items.stream()
          .map(i -> new NewsItem(
              i.id() != null ? i.id().toString() : "",
              i.title() != null ? i.title() : "",
              i.content() != null ? i.content() : "",
              i.sourceUrl() != null ? i.sourceUrl() : "",
              i.sourceType() != null ? i.sourceType().name() : "",
              i.publishedAt() != null ? i.publishedAt().toString() : ""))
          .toList();
      return new PublicContentPayload(news, news.size());
    } catch (RuntimeException e) {
      log.warn("platform_admin_dashboard: public content unavailable — {}", e.getMessage());
      return PublicContentPayload.empty();
    }
  }

  private QuickActionsPayload buildQuickActions() {
    return new QuickActionsPayload(List.of(
        new ActionItem("CREATE_TENANT",     "quickaction.platform.create_tenant",     "add_business",  "/app/platform/tenants/new"),
        new ActionItem("TENANT_ONBOARDING", "quickaction.platform.onboarding",        "playlist_add_check", "/app/platform/tenants/onboarding"),
        new ActionItem("SUBSCRIPTIONS",     "quickaction.platform.subscriptions",     "workspace_premium", "/app/platform/subscriptions"),
        new ActionItem("PLATFORM_HEALTH",   "quickaction.platform.platform_health",   "monitor_heart", "/app/platform/health"),
        new ActionItem("DRAW_RESULTS",      "quickaction.platform.draw_results",      "fact_check", "/app/platform/ops/draw-results")));
  }

  public record Payload(
      PlatformHealthPayload health,
      TenantKpiSummaryPayload tenants,
      SubscriptionSummaryPayload subscriptions,
      OnboardingAlertsPayload onboardingAlerts,
      PlatformAlertsPayload platformAlerts,
      PublicContentPayload publicContent,
      QuickActionsPayload quickActions) {}

  // ---- surface-specific typed payload records ----

  /** Platform health snapshot — global status + per-component status map. */
  public record PlatformHealthPayload(
      String global,
      Map<String, String> components) {}

  /** Single tenant row in the "top tenants by volume" ranking. */
  public record TenantRankItem(
      String tenantCode,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal netRevenue) {}

  /** Tenant KPI summary — counts + analytics for today. */
  public record TenantKpiSummaryPayload(
      long total,
      long active,
      long suspended,
      long onboarding,
      long actionRequired,
      long ticketsSoldToday,
      BigDecimal grossSalesToday,
      BigDecimal netToday,
      List<TenantRankItem> topTenants) {}

  /** Subscription counts across all tenants. {@code byPlan} is the raw catalog projection. */
  public record SubscriptionSummaryPayload(
      long active,
      long trial,
      long pastDue,
      long expired,
      long total,
      List<?> byPlan) {

    public static SubscriptionSummaryPayload empty() {
      return new SubscriptionSummaryPayload(0L, 0L, 0L, 0L, 0L, List.of());
    }
  }

  /** Single tenant currently in DRAFT / onboarding state. */
  public record OnboardingAlertItem(
      String tenantId,
      String code,
      String name,
      String status) {}

  /** List of tenants in onboarding — up to 10. */
  public record OnboardingAlertsPayload(
      List<OnboardingAlertItem> items,
      int count) {

    public static OnboardingAlertsPayload empty() {
      return new OnboardingAlertsPayload(List.of(), 0);
    }
  }

  /** Platform-wide alerts placeholder (TODO V2 — no aggregate query yet). */
  public record PlatformAlertsPayload(
      List<AlertItem> items,
      int count) {

    public static PlatformAlertsPayload empty() {
      return new PlatformAlertsPayload(List.of(), 0);
    }
  }
}
