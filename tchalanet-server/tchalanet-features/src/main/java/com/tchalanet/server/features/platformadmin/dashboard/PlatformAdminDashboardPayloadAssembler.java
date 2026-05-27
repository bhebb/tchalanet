package com.tchalanet.server.features.platformadmin.dashboard;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatsView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.subscription.api.query.GetPlatformSubscriptionStatsQuery;
import com.tchalanet.server.core.subscription.api.query.PlatformSubscriptionStatsView;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardModels.PlatformDashboardStatsCriteria;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardModels.PlatformDashboardStatsResponse;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardModels.PlatformSummaryCard;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardStatsService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code platform_admin_dashboard}.
 * One assembly per request — memoized via {@code PageModelResolutionContext}.
 *
 * Grouped reads (target ≤ 4 per dashboard-overview-runtime-v1 §12):
 *   1. HealthEndpoint.health()              (in-process, no SQL)
 *   2. TenantCatalog.stats() + PlatformDashboardStatsService.getStats()  (tenants KPI bundle)
 *   3. QueryBus.ask(GetPlatformSubscriptionStatsQuery)                   (subscriptions)
 *   4. TenantCatalog.listTenants(size=10)                                (onboarding alerts)
 *
 * Platform alerts placeholder: no cross-platform alert aggregate exists yet (TODO V2).
 */
@Component
@RequiredArgsConstructor
public class PlatformAdminDashboardPayloadAssembler {

  private final PlatformDashboardStatsService statsService;
  private final TenantCatalog tenantCatalog;
  private final QueryBus queryBus;
  /** Optional — adapter typically lives in the app layer (wraps actuator HealthEndpoint). */
  private final ObjectProvider<PlatformHealthProbe> healthProbeProvider;

  public Payload assemble(TchRequestContext ctx) {
    Map<String, Object> health = buildHealth();
    Map<String, Object> tenants = buildTenantsKpi();
    Map<String, Object> subscriptions = buildSubscriptions();
    Map<String, Object> onboardingAlerts = buildOnboardingAlerts();
    Map<String, Object> platformAlerts = buildPlatformAlerts();
    Map<String, Object> quickActions = buildQuickActions();

    return new Payload(health, tenants, subscriptions, onboardingAlerts, platformAlerts, quickActions);
  }

  /**
   * Platform health snapshot from Spring Boot's {@link HealthEndpoint}.
   * No SQL — reads the in-process health registry that the actuator already maintains.
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

  private Map<String, Object> buildTenantsKpi() {
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
      // leave at zero — payload shape stays stable
    }

    try {
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      PlatformDashboardStatsResponse stats =
          statsService.getStats(new PlatformDashboardStatsCriteria(today, today));
      PlatformSummaryCard summary = stats != null ? stats.summary() : null;
      if (summary != null && total == 0L) {
        // fall back to platform stats when catalog stats are empty (no view materialized yet)
        total = summary.totalTenants();
      }
    } catch (RuntimeException e) {
      // ignore — keep catalog values
    }

    long actionRequired = suspended;
    return Map.of(
        "total", total,
        "active", active,
        "suspended", suspended,
        "onboarding", onboarding,
        "actionRequired", actionRequired);
  }

  private Map<String, Object> buildSubscriptions() {
    try {
      PlatformSubscriptionStatsView view = queryBus.ask(new GetPlatformSubscriptionStatsQuery());
      if (view != null) {
        return Map.of(
            "active", (long) view.active(),
            "trial", 0L,
            "pastDue", (long) view.pastDue(),
            "expired", (long) view.canceled(),
            "total", (long) view.total(),
            "byPlan", view.byPlan() != null ? view.byPlan() : List.of());
      }
    } catch (RuntimeException e) {
      // fall through to placeholder
    }
    return Map.of(
        "active", 0L,
        "trial", 0L,
        "pastDue", 0L,
        "expired", 0L,
        "total", 0L,
        "byPlan", List.of());
  }

  /**
   * Tenants currently in DRAFT (onboarding in progress) — up to 10.
   * Uses {@link TenantCatalog#listTenants} which returns all tenants paginated;
   * filtered in-memory because the catalog API has no status filter today.
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
                "code", t.code(),
                "name", t.name() != null ? t.name() : "",
                "status", t.status().name()));
          }
        }
        return Map.of("items", items, "count", items.size());
      }
    } catch (RuntimeException e) {
      // fall through
    }
    return Map.of("items", List.of(), "count", 0);
  }

  /** V1 placeholder: no cross-platform alerts aggregate yet (TODO V2). */
  private Map<String, Object> buildPlatformAlerts() {
    return Map.of("items", List.of(), "count", 0);
  }

  private Map<String, Object> buildQuickActions() {
    return Map.of(
        "actions", List.of(
            Map.of("id", "CREATE_TENANT", "label", "Créer un tenant", "route", "/app/platform/tenants/new"),
            Map.of("id", "PLATFORM_OVERVIEW", "label", "Aperçu plateforme", "route", "/app/platform/overview"),
            Map.of("id", "OPS_HEALTH", "label", "Santé plateforme", "route", "/app/platform/ops/health")));
  }

  public record Payload(
      Map<String, Object> health,
      Map<String, Object> tenants,
      Map<String, Object> subscriptions,
      Map<String, Object> onboardingAlerts,
      Map<String, Object> platformAlerts,
      Map<String, Object> quickActions) {}
}
