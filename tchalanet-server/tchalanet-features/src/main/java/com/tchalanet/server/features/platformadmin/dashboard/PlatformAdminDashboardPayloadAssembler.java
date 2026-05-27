package com.tchalanet.server.features.platformadmin.dashboard;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardModels.PlatformDashboardStatsCriteria;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardModels.PlatformDashboardStatsResponse;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardModels.PlatformSummaryCard;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardStatsService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code platform_admin_dashboard}.
 * One assembly per request — memoized via {@code PageModelResolutionContext}.
 *
 * Grouped reads (target ≤ 4 per dashboard-overview-runtime-v1 §12):
 *   1. platform stats (PlatformDashboardStatsService — tenants KPI)
 *
 * Health / subscriptions / onboarding / platform alerts are V1 placeholders
 * (wired to dedicated platform queries in later iterations).
 */
@Component
@RequiredArgsConstructor
public class PlatformAdminDashboardPayloadAssembler {

  private final PlatformDashboardStatsService statsService;

  public Payload assemble(TchRequestContext ctx) {
    Map<String, Object> health = buildHealth();
    Map<String, Object> tenants = buildTenantsKpi();
    Map<String, Object> subscriptions = buildSubscriptions();
    Map<String, Object> onboardingAlerts = buildOnboardingAlerts();
    Map<String, Object> platformAlerts = buildPlatformAlerts();
    Map<String, Object> quickActions = buildQuickActions();

    return new Payload(health, tenants, subscriptions, onboardingAlerts, platformAlerts, quickActions);
  }

  private Map<String, Object> buildTenantsKpi() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    PlatformDashboardStatsResponse stats =
        statsService.getStats(new PlatformDashboardStatsCriteria(today, today));

    PlatformSummaryCard summary = stats != null ? stats.summary() : null;
    long totalTenants = summary != null ? summary.totalTenants() : 0L;

    return Map.of(
        "total", totalTenants,
        "active", totalTenants,
        "suspended", 0L,
        "onboarding", 0L,
        "actionRequired", 0L);
  }

  /** V1: platform health placeholder. Wire to platform health checks later. */
  private Map<String, Object> buildHealth() {
    return Map.of(
        "api", "UNKNOWN",
        "db", "UNKNOWN",
        "keycloak", "UNKNOWN",
        "cache", "UNKNOWN",
        "scheduler", "UNKNOWN",
        "notifications", "UNKNOWN");
  }

  /** V1: subscriptions placeholder. */
  private Map<String, Object> buildSubscriptions() {
    return Map.of(
        "active", 0L,
        "trial", 0L,
        "pastDue", 0L,
        "expired", 0L);
  }

  /** V1: onboarding alerts placeholder. */
  private Map<String, Object> buildOnboardingAlerts() {
    return Map.of(
        "items", List.of(),
        "count", 0);
  }

  /** V1: platform alerts placeholder. */
  private Map<String, Object> buildPlatformAlerts() {
    return Map.of(
        "items", List.of(),
        "count", 0);
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
