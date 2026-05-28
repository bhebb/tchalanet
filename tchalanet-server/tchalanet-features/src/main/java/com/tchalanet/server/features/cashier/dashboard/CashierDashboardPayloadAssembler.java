package com.tchalanet.server.features.cashier.dashboard;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.core.analytics.api.model.CashierDashboardStatsView;
import com.tchalanet.server.core.analytics.api.query.GetCashierDashboardStatsQuery;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.core.sales.api.query.GetCashierDashboardOverviewQuery;
import com.tchalanet.server.core.sales.api.query.ListCashierRecentTicketsQuery;
import com.tchalanet.server.core.session.api.query.GetCashierIdentityQuery;
import com.tchalanet.server.core.session.api.query.GetCashierSessionSummaryQuery;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code cashier_dashboard}.
 * One assembly per request — memoized via {@code PageModelResolutionContext}.
 *
 * Grouped reads (target ≤ 4 per dashboard-overview-runtime-v1 §12) :
 *   1. identity       — {@link GetCashierIdentityQuery}
 *   2. session        — {@link GetCashierSessionSummaryQuery}
 *   3. overview       — {@link GetCashierDashboardOverviewQuery} (only when session active)
 *   4. nextDraws      — {@link ListCashierNextDrawsQuery}
 *   5. recentTickets  — {@link ListCashierRecentTicketsQuery}
 *
 * Note: overview is conditional on an active session — most-common case is therefore
 * ≤ 4 grouped reads. When a session is open, count goes to 5 (one over target, which
 * is acceptable for V1 — the overview query is materially cheap).
 *
 * Readiness and alerts are derived from {@link OperationalContextHint} already
 * carried by the HTTP request — no extra read.
 */
@Component
@RequiredArgsConstructor
public class CashierDashboardPayloadAssembler {

  private static final int DEFAULT_NEXT_DRAWS_LIMIT = 8;
  private static final int DEFAULT_NEXT_DRAWS_LOOKAHEAD_HOURS = 48;
  private static final int DEFAULT_RECENT_TICKETS_LIMIT = 10;

  private final QueryBus queryBus;

  public Payload assemble(TchRequestContext ctx) {
    if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
      return Payload.empty();
    }

    Map<String, Object> identity = loadIdentity(ctx);
    Map<String, Object> session = loadSession(ctx);
    Map<String, Object> overview = loadOverview(ctx, session);
    List<?> nextDraws = loadNextDraws();
    List<?> recentTickets = loadRecentTickets(ctx);
    Map<String, Object> readiness = buildReadiness(ctx.operationalContext());
    Map<String, Object> alerts = buildAlerts(ctx.operationalContext(), session);
    Map<String, Object> stats = loadAnalyticsStats(ctx);
    Map<String, Object> offlineSync = buildOfflineSyncPlaceholder();

    return new Payload(identity, session, overview, nextDraws, recentTickets, readiness, alerts, stats, offlineSync);
  }

  private Map<String, Object> loadIdentity(TchRequestContext ctx) {
    var view = queryBus.ask(new GetCashierIdentityQuery(ctx.tenantId(), ctx.userId()));
    if (view == null) return Map.of();
    var result = new HashMap<String, Object>();
    if (view.cashierDisplayName() != null) result.put("cashierDisplayName", view.cashierDisplayName());
    if (view.outletName() != null)         result.put("outletName", view.outletName());
    if (view.terminalLabel() != null)      result.put("terminalLabel", view.terminalLabel());
    if (view.tenantCode() != null)         result.put("tenantCode", view.tenantCode());
    return result;
  }

  private Map<String, Object> loadSession(TchRequestContext ctx) {
    var view = queryBus.ask(new GetCashierSessionSummaryQuery(ctx.tenantId(), ctx.userId()));
    if (view == null || !view.active()) {
      return Map.of("active", false);
    }
    return Map.of(
        "active", true,
        "sessionRef", view.sessionRef() != null ? view.sessionRef() : "",
        "openedAt", view.openedAt() != null ? view.openedAt().toString() : "",
        "openingFloatCents", view.openingFloatCents(),
        "salesTotalCents", view.salesTotalCents(),
        "ticketCount", view.ticketCount());
  }

  private Map<String, Object> loadOverview(TchRequestContext ctx, Map<String, Object> session) {
    if (!Boolean.TRUE.equals(session.get("active"))) {
      return Map.of(
          "sessionOpen", false,
          "ticketCount", 0L,
          "salesTotalCents", 0L,
          "cancelledCount", 0L,
          "pendingApprovalCount", 0L);
    }

    String openedAtStr = (String) session.getOrDefault("openedAt", "");
    LocalDate businessDate = !openedAtStr.isBlank()
        ? java.time.Instant.parse(openedAtStr).atZone(ZoneOffset.UTC).toLocalDate()
        : LocalDate.now(ZoneOffset.UTC);

    var overview = queryBus.ask(
        new GetCashierDashboardOverviewQuery(ctx.tenantId(), ctx.userId(), businessDate));

    var result = new HashMap<String, Object>();
    result.put("sessionOpen", true);
    result.put("sessionRef", session.getOrDefault("sessionRef", ""));
    result.put("openedAt", openedAtStr);
    result.put("businessDate", businessDate.toString());
    result.put("ticketCount", overview != null ? overview.ticketCount() : 0L);
    result.put("salesTotalCents", overview != null ? overview.salesTotalCents() : 0L);
    result.put("cancelledCount", overview != null ? overview.cancelledCount() : 0L);
    result.put("pendingApprovalCount", overview != null ? overview.pendingApprovalCount() : 0L);
    result.put("byDraw", overview != null && overview.byDraw() != null ? overview.byDraw() : List.of());
    return result;
  }

  private List<?> loadNextDraws() {
    var items = queryBus.ask(
        new ListCashierNextDrawsQuery(DEFAULT_NEXT_DRAWS_LOOKAHEAD_HOURS, DEFAULT_NEXT_DRAWS_LIMIT));
    return items != null ? items : List.of();
  }

  private List<?> loadRecentTickets(TchRequestContext ctx) {
    var items = queryBus.ask(
        new ListCashierRecentTicketsQuery(ctx.userId(), DEFAULT_RECENT_TICKETS_LIMIT));
    return items != null ? items : List.of();
  }

  /**
   * Pre-aggregated analytics stats for today (seller scope).
   * Falls back to empty if analytics data is not yet available.
   */
  private Map<String, Object> loadAnalyticsStats(TchRequestContext ctx) {
    try {
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      CashierDashboardStatsView view = queryBus.ask(
          new GetCashierDashboardStatsQuery(ctx.tenantId(), ctx.userId(), today));
      if (view == null || view.today() == null) {
        return Map.of("available", false);
      }
      var card = view.today();
      var result = new HashMap<String, Object>();
      result.put("available", true);
      result.put("refDate", view.refDate() != null ? view.refDate().toString() : today.toString());
      result.put("ticketsSold", card.ticketsSold());
      result.put("grossSales", card.grossSales() != null ? card.grossSales() : 0);
      result.put("winningsCalculated", card.winningsCalculated() != null ? card.winningsCalculated() : 0);
      result.put("netRevenue", card.netRevenueEstimated() != null ? card.netRevenueEstimated() : 0);
      result.put("gameBreakdown", view.gameBreakdown() != null ? view.gameBreakdown().stream()
          .map(g -> Map.<String, Object>of(
              "gameCode", g.gameCode() != null ? g.gameCode() : "",
              "ticketsSold", g.ticketsSold(),
              "grossSales", g.grossSales() != null ? g.grossSales() : 0))
          .toList() : List.of());
      return result;
    } catch (RuntimeException e) {
      return Map.of("available", false);
    }
  }

  /** V1 placeholder — offline/sync status is not yet tracked server-side. */
  private Map<String, Object> buildOfflineSyncPlaceholder() {
    return Map.of("status", "UNKNOWN", "pendingSyncCount", 0);
  }

  /**
   * Operational readiness derived from {@link OperationalContextHint} carried by the
   * HTTP context — no extra DB read.
   */
  private Map<String, Object> buildReadiness(OperationalContextHint hint) {
    List<String> missing = new ArrayList<>();
    if (hint == null || hint.outletId() == null)   missing.add("OUTLET");
    if (hint == null || hint.terminalId() == null) missing.add("TERMINAL");

    boolean trusted = hint != null && hint.trustedForSensitiveOperation();
    boolean ready = missing.isEmpty() && trusted;

    return Map.of(
        "ready", ready,
        "trusted", trusted,
        "source", hint != null && hint.source() != null ? hint.source().name() : "NONE",
        "missing", List.copyOf(missing));
  }

  /**
   * Operational alerts (blockers/warnings) — combines context flags + session state.
   * No extra DB read; uses the data already loaded by session + the hint.
   */
  private Map<String, Object> buildAlerts(
      OperationalContextHint hint, Map<String, Object> session) {
    List<Map<String, Object>> warnings = new ArrayList<>();

    if (hint == null || hint.outletId() == null) {
      warnings.add(Map.of("severity", "BLOCKER", "code", "OUTLET_MISSING",
          "messageKey", "alert.cashier.outlet_missing"));
    }
    if (hint == null || hint.terminalId() == null) {
      warnings.add(Map.of("severity", "BLOCKER", "code", "TERMINAL_MISSING",
          "messageKey", "alert.cashier.terminal_missing"));
    }
    if (hint != null && !hint.trustedForSensitiveOperation()) {
      warnings.add(Map.of("severity", "WARNING", "code", "CONTEXT_UNTRUSTED",
          "messageKey", "alert.cashier.context_untrusted"));
    }
    if (!Boolean.TRUE.equals(session.get("active"))) {
      warnings.add(Map.of("severity", "WARNING", "code", "SESSION_CLOSED",
          "messageKey", "alert.cashier.session_closed"));
    }

    return Map.of(
        "count", warnings.size(),
        "items", List.copyOf(warnings));
  }

  public record Payload(
      Map<String, Object> identity,
      Map<String, Object> session,
      Map<String, Object> overview,
      List<?> nextDraws,
      List<?> recentTickets,
      Map<String, Object> readiness,
      Map<String, Object> alerts,
      Map<String, Object> stats,
      Map<String, Object> offlineSync) {

    public static Payload empty() {
      return new Payload(
          Map.of(),
          Map.of("active", false),
          Map.of(),
          List.of(),
          List.of(),
          Map.of("ready", false, "trusted", false, "source", "NONE",
              "missing", List.of("OUTLET", "TERMINAL")),
          Map.of("count", 0, "items", List.of()),
          Map.of("available", false),
          Map.of("status", "UNKNOWN", "pendingSyncCount", 0));
    }
  }
}
