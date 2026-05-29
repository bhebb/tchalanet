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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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

    CashierIdentityPayload identity    = loadIdentity(ctx);
    CashierSessionPayload session      = loadSession(ctx);
    CashierOverviewPayload overview    = loadOverview(ctx, session);
    List<?> nextDraws                  = loadNextDraws();
    List<?> recentTickets              = loadRecentTickets(ctx);
    CashierReadinessPayload readiness  = buildReadiness(ctx.operationalContext());
    CashierAlertsPayload alerts        = buildAlerts(ctx.operationalContext(), session);
    CashierStatsPayload stats          = loadAnalyticsStats(ctx);
    CashierOfflineSyncPayload offline  = buildOfflineSyncPlaceholder();

    return new Payload(identity, session, overview, nextDraws, recentTickets,
        readiness, alerts, stats, offline);
  }

  private CashierIdentityPayload loadIdentity(TchRequestContext ctx) {
    var view = queryBus.ask(new GetCashierIdentityQuery(ctx.tenantId(), ctx.userId()));
    if (view == null) {
      return new CashierIdentityPayload("", "", "", "");
    }
    return new CashierIdentityPayload(
        view.cashierDisplayName() != null ? view.cashierDisplayName() : "",
        view.outletName() != null ? view.outletName() : "",
        view.terminalLabel() != null ? view.terminalLabel() : "",
        view.tenantCode() != null ? view.tenantCode() : "");
  }

  private CashierSessionPayload loadSession(TchRequestContext ctx) {
    var view = queryBus.ask(new GetCashierSessionSummaryQuery(ctx.tenantId(), ctx.userId()));
    if (view == null || !view.active()) {
      return CashierSessionPayload.inactive();
    }
    return new CashierSessionPayload(
        true,
        view.sessionRef() != null ? view.sessionRef() : "",
        view.openedAt() != null ? view.openedAt().toString() : "",
        view.openingFloatCents(),
        view.salesTotalCents(),
        view.ticketCount());
  }

  private CashierOverviewPayload loadOverview(TchRequestContext ctx, CashierSessionPayload session) {
    if (!session.active()) {
      return CashierOverviewPayload.noSession();
    }

    LocalDate businessDate = !session.openedAt().isBlank()
        ? java.time.Instant.parse(session.openedAt()).atZone(ZoneOffset.UTC).toLocalDate()
        : LocalDate.now(ZoneOffset.UTC);

    var view = queryBus.ask(
        new GetCashierDashboardOverviewQuery(ctx.tenantId(), ctx.userId(), businessDate));

    return new CashierOverviewPayload(
        true,
        session.sessionRef(),
        session.openedAt(),
        businessDate.toString(),
        view != null ? view.ticketCount() : 0L,
        view != null ? view.salesTotalCents() : 0L,
        view != null ? view.cancelledCount() : 0L,
        view != null ? view.pendingApprovalCount() : 0L,
        view != null && view.byDraw() != null ? view.byDraw() : List.of());
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
  private CashierStatsPayload loadAnalyticsStats(TchRequestContext ctx) {
    try {
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      CashierDashboardStatsView view = queryBus.ask(
          new GetCashierDashboardStatsQuery(ctx.tenantId(), ctx.userId(), today));
      if (view == null || view.today() == null) {
        return CashierStatsPayload.unavailable();
      }
      var card = view.today();
      List<GameBreakdownItem> breakdown = List.of();
      if (view.gameBreakdown() != null) {
        breakdown = view.gameBreakdown().stream()
            .map(g -> new GameBreakdownItem(
                g.gameCode() != null ? g.gameCode() : "",
                g.ticketsSold(),
                g.grossSales() != null ? g.grossSales() : BigDecimal.ZERO))
            .toList();
      }
      return new CashierStatsPayload(
          true,
          view.refDate() != null ? view.refDate().toString() : today.toString(),
          card.ticketsSold(),
          card.grossSales() != null ? card.grossSales() : BigDecimal.ZERO,
          card.winningsCalculated() != null ? card.winningsCalculated() : BigDecimal.ZERO,
          card.netRevenueEstimated() != null ? card.netRevenueEstimated() : BigDecimal.ZERO,
          breakdown);
    } catch (RuntimeException e) {
      return CashierStatsPayload.unavailable();
    }
  }

  /** V1 placeholder — offline/sync status is not yet tracked server-side. */
  private CashierOfflineSyncPayload buildOfflineSyncPlaceholder() {
    return new CashierOfflineSyncPayload("UNKNOWN", 0);
  }

  /**
   * Operational readiness derived from {@link OperationalContextHint} carried by the
   * HTTP context — no extra DB read.
   */
  private CashierReadinessPayload buildReadiness(OperationalContextHint hint) {
    List<String> missing = new ArrayList<>();
    if (hint == null || hint.outletId() == null)   missing.add("OUTLET");
    if (hint == null || hint.terminalId() == null) missing.add("TERMINAL");

    boolean trusted = hint != null && hint.trustedForSensitiveOperation();
    boolean ready = missing.isEmpty() && trusted;

    return new CashierReadinessPayload(
        ready,
        trusted,
        hint != null && hint.source() != null ? hint.source().name() : "NONE",
        List.copyOf(missing));
  }

  /**
   * Operational alerts (blockers/warnings) — combines context flags + session state.
   * No extra DB read; uses the data already loaded by session + the hint.
   */
  private CashierAlertsPayload buildAlerts(
      OperationalContextHint hint, CashierSessionPayload session) {
    List<CashierAlertItem> warnings = new ArrayList<>();

    if (hint == null || hint.outletId() == null) {
      warnings.add(new CashierAlertItem("BLOCKER", "OUTLET_MISSING",
          "alert.cashier.outlet_missing"));
    }
    if (hint == null || hint.terminalId() == null) {
      warnings.add(new CashierAlertItem("BLOCKER", "TERMINAL_MISSING",
          "alert.cashier.terminal_missing"));
    }
    if (hint != null && !hint.trustedForSensitiveOperation()) {
      warnings.add(new CashierAlertItem("WARNING", "CONTEXT_UNTRUSTED",
          "alert.cashier.context_untrusted"));
    }
    if (!session.active()) {
      warnings.add(new CashierAlertItem("WARNING", "SESSION_CLOSED",
          "alert.cashier.session_closed"));
    }

    return new CashierAlertsPayload(warnings.size(), List.copyOf(warnings));
  }

  // ---- Payload record ----

  public record Payload(
      CashierIdentityPayload identity,
      CashierSessionPayload session,
      CashierOverviewPayload overview,
      List<?> nextDraws,
      List<?> recentTickets,
      CashierReadinessPayload readiness,
      CashierAlertsPayload alerts,
      CashierStatsPayload stats,
      CashierOfflineSyncPayload offlineSync) {

    public static Payload empty() {
      return new Payload(
          new CashierIdentityPayload("", "", "", ""),
          CashierSessionPayload.inactive(),
          CashierOverviewPayload.noSession(),
          List.of(),
          List.of(),
          new CashierReadinessPayload(false, false, "NONE", List.of("OUTLET", "TERMINAL")),
          new CashierAlertsPayload(0, List.of()),
          CashierStatsPayload.unavailable(),
          new CashierOfflineSyncPayload("UNKNOWN", 0));
    }
  }

  // ---- surface-specific typed payload records ----

  /** Cashier identity — display name, outlet, terminal, tenant context. */
  public record CashierIdentityPayload(
      String cashierDisplayName,
      String outletName,
      String terminalLabel,
      String tenantCode) {}

  /** Cashier session state. */
  public record CashierSessionPayload(
      boolean active,
      String sessionRef,
      String openedAt,
      long openingFloatCents,
      long salesTotalCents,
      long ticketCount) {

    public static CashierSessionPayload inactive() {
      return new CashierSessionPayload(false, "", "", 0L, 0L, 0L);
    }
  }

  /** Sales overview for the current session / business date. */
  public record CashierOverviewPayload(
      boolean sessionOpen,
      String sessionRef,
      String openedAt,
      String businessDate,
      long ticketCount,
      long salesTotalCents,
      long cancelledCount,
      long pendingApprovalCount,
      List<?> byDraw) {

    public static CashierOverviewPayload noSession() {
      return new CashierOverviewPayload(false, "", "", "", 0L, 0L, 0L, 0L, List.of());
    }
  }

  /** Operational readiness flags derived from HTTP context — no extra DB read. */
  public record CashierReadinessPayload(
      boolean ready,
      boolean trusted,
      String source,
      List<String> missing) {}

  /** Single operational alert item. */
  public record CashierAlertItem(
      String severity,
      String code,
      String messageKey) {}

  /** Cashier alerts summary (blockers + warnings). */
  public record CashierAlertsPayload(
      int count,
      List<CashierAlertItem> items) {}

  /** Per-game breakdown inside analytics stats. */
  public record GameBreakdownItem(
      String gameCode,
      long ticketsSold,
      BigDecimal grossSales) {}

  /** Analytics stats for today (seller scope). */
  public record CashierStatsPayload(
      boolean available,
      String refDate,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal winningsCalculated,
      BigDecimal netRevenue,
      List<GameBreakdownItem> gameBreakdown) {

    public static CashierStatsPayload unavailable() {
      return new CashierStatsPayload(false, "", 0L,
          BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }
  }

  /** Offline/sync placeholder — V1, no server-side sync tracking yet. */
  public record CashierOfflineSyncPayload(
      String status,
      int pendingSyncCount) {}
}
