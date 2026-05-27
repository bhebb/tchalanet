package com.tchalanet.server.features.tenantadmin.dashboard;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.stats.tenantdashboard.app.TenantDashboardStatsService;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantDashboardStatsView;
import com.tchalanet.server.features.stats.tenantdashboard.model.TenantSummaryCard;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code tenant_admin_dashboard}.
 * One assembly per request — memoized via {@code PageModelResolutionContext}.
 *
 * Grouped reads (target ≤ 5 per dashboard-overview-runtime-v1 §12):
 *   1. header info (tenant context)
 *   2. tenant dashboard stats (TenantDashboardStatsService — KPI day-window)
 *
 * Readiness/alerts/summaries are placeholder maps in V1; they are enriched in
 * Vague 5 once GetTenantReadinessQuery is in place.
 */
@Component
@RequiredArgsConstructor
public class TenantAdminDashboardPayloadAssembler {

  private final TenantDashboardStatsService statsService;

  public Payload assemble(TchRequestContext ctx) {
    if (ctx == null || ctx.tenantId() == null) {
      return Payload.empty();
    }

    Map<String, Object> header = buildHeader(ctx);
    Map<String, Object> kpis = buildKpis(ctx);
    Map<String, Object> readiness = buildReadinessSummary();
    Map<String, Object> alerts = buildAlerts();
    Map<String, Object> operations = buildOperationsSummary();
    Map<String, Object> commercial = buildCommercialSummary();
    Map<String, Object> quickActions = buildQuickActions();

    return new Payload(header, kpis, readiness, alerts, operations, commercial, quickActions);
  }

  private Map<String, Object> buildHeader(TchRequestContext ctx) {
    return Map.of(
        "tenantCode", ctx.effectiveTenantCode() != null ? ctx.effectiveTenantCode() : "",
        "tenantId", ctx.tenantId() != null ? ctx.tenantId().value().toString() : "",
        "timezone", ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : "UTC",
        "currency", ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "");
  }

  private Map<String, Object> buildKpis(TchRequestContext ctx) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    var response = statsService.getStats(ctx.tenantId(), today, today);
    TenantDashboardStatsView stats = response != null ? response.stats() : null;
    TenantSummaryCard summary = stats != null ? stats.summary() : null;

    return Map.of(
        "salesToday", summary != null && summary.totalSales() != null
            ? summary.totalSales() : BigDecimal.ZERO,
        "ticketCountToday", summary != null ? summary.ticketsSold() : 0L,
        "activeSessions", 0L,
        "openDraws", 0L,
        "pendingApprovals", 0L);
  }

  /**
   * V1: empty placeholder. Wired to GetTenantReadinessQuery in Vague 5.
   */
  private Map<String, Object> buildReadinessSummary() {
    return Map.of(
        "status", "UNKNOWN",
        "missingCount", 0,
        "topIssues", List.of());
  }

  /**
   * V1: empty placeholder. Wire to notification summary when available.
   */
  private Map<String, Object> buildAlerts() {
    return Map.of(
        "unreadCount", 0,
        "topWarnings", List.of());
  }

  private Map<String, Object> buildOperationsSummary() {
    return Map.of(
        "users", Map.of("status", "UNKNOWN", "count", 0),
        "outlets", Map.of("status", "UNKNOWN", "count", 0),
        "terminals", Map.of("status", "UNKNOWN", "count", 0),
        "sessions", Map.of("status", "UNKNOWN", "count", 0));
  }

  private Map<String, Object> buildCommercialSummary() {
    return Map.of(
        "gamesPricing", Map.of("status", "UNKNOWN"),
        "drawChannels", Map.of("status", "UNKNOWN"),
        "limits", Map.of("status", "UNKNOWN"),
        "promotions", Map.of("status", "UNKNOWN"));
  }

  private Map<String, Object> buildQuickActions() {
    return Map.of(
        "actions", List.of(
            Map.of("id", "CREATE_OUTLET", "label", "Créer un point de vente", "route", "/app/admin/outlets/new"),
            Map.of("id", "CREATE_TERMINAL", "label", "Créer un terminal", "route", "/app/admin/terminals/new"),
            Map.of("id", "CREATE_SELLER", "label", "Créer un vendeur", "route", "/app/admin/users/new"),
            Map.of("id", "TENANT_OVERVIEW", "label", "Aperçu du tenant", "route", "/app/admin/overview")));
  }

  public record Payload(
      Map<String, Object> header,
      Map<String, Object> kpis,
      Map<String, Object> readiness,
      Map<String, Object> alerts,
      Map<String, Object> operations,
      Map<String, Object> commercial,
      Map<String, Object> quickActions) {

    public static Payload empty() {
      return new Payload(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }
  }
}
