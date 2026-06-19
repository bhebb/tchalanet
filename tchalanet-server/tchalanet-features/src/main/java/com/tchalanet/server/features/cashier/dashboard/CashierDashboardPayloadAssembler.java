package com.tchalanet.server.features.cashier.dashboard;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.analytics.api.model.CashierDashboardStatsView;
import com.tchalanet.server.core.analytics.api.query.GetCashierDashboardStatsQuery;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.core.sales.api.query.GetCashierDashboardOverviewQuery;
import com.tchalanet.server.core.sales.api.query.ListCashierRecentTicketsQuery;
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
 *   1. overview       — {@link GetCashierDashboardOverviewQuery}
 *   2. nextDraws      — {@link ListCashierNextDrawsQuery}
 *   3. recentTickets  — {@link ListCashierRecentTicketsQuery}
 *
 * Readiness and alerts are derived from the seller-terminal context already carried by the HTTP
 * request — no extra read.
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
    CashierSessionPayload session      = CashierSessionPayload.v0();
    CashierOverviewPayload overview    = loadOverview(ctx);
    List<?> nextDraws                  = loadNextDraws();
    List<?> recentTickets              = loadRecentTickets(ctx);
    CashierReadinessPayload readiness  = buildReadiness(ctx);
    CashierAlertsPayload alerts        = buildAlerts(ctx);
    CashierStatsPayload stats          = loadAnalyticsStats(ctx);
    CashierOfflineSyncPayload offline  = buildOfflineSyncPlaceholder();

    return new Payload(identity, session, overview, nextDraws, recentTickets,
        readiness, alerts, stats, offline);
  }

  private CashierIdentityPayload loadIdentity(TchRequestContext ctx) {
    return new CashierIdentityPayload(
        ctx.sellerTerminalId() != null ? ctx.sellerTerminalId().value().toString() : "",
        "",
        "",
        ctx.effectiveTenantCode() != null ? ctx.effectiveTenantCode() : "");
  }

  private CashierOverviewPayload loadOverview(TchRequestContext ctx) {
    LocalDate businessDate = LocalDate.now(ZoneOffset.UTC);

    var view = queryBus.ask(
        new GetCashierDashboardOverviewQuery(ctx.tenantId(), ctx.userId(), businessDate));

    return new CashierOverviewPayload(
        true,
        "",
        "",
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

  /** V1 placeholder — offline/sync status is not tracked in V0. */
  private CashierOfflineSyncPayload buildOfflineSyncPlaceholder() {
    return new CashierOfflineSyncPayload("PARKED", 0);
  }

  /**
   * Operational readiness derived from seller-terminal context carried by the HTTP context.
   */
  private CashierReadinessPayload buildReadiness(TchRequestContext ctx) {
    List<String> missing = new ArrayList<>();
    if (ctx == null || ctx.sellerTerminalId() == null) missing.add("SELLER_TERMINAL");

    boolean trusted = missing.isEmpty();
    boolean ready = missing.isEmpty() && trusted;

    return new CashierReadinessPayload(
        ready,
        trusted,
        trusted ? "SELLER_TERMINAL" : "NONE",
        List.copyOf(missing));
  }

  /**
   * Operational alerts (blockers/warnings) — derived from context flags only.
   */
  private CashierAlertsPayload buildAlerts(TchRequestContext ctx) {
    List<CashierAlertItem> warnings = new ArrayList<>();

    if (ctx == null || ctx.sellerTerminalId() == null) {
      warnings.add(new CashierAlertItem("BLOCKER", "SELLER_TERMINAL_MISSING",
          "alert.cashier.seller_terminal_missing"));
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
          new CashierReadinessPayload(false, false, "NONE", List.of("SELLER_TERMINAL")),
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

    public static CashierSessionPayload v0() {
      return new CashierSessionPayload(true, "", "", 0L, 0L, 0L);
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
