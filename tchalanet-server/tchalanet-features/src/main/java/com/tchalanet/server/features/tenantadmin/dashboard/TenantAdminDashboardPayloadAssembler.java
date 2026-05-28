package com.tchalanet.server.features.tenantadmin.dashboard;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.outlet.api.query.ListOutletsByTenantQuery;
import com.tchalanet.server.core.outlet.api.query.OutletView;
import com.tchalanet.server.core.seller.api.query.ListSellersQuery;
import com.tchalanet.server.core.seller.api.query.model.SellerSummaryView;
import com.tchalanet.server.core.terminal.api.query.ListTerminalsQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessIssue;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessStatus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSummary;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code tenant_admin_dashboard}.
 * One assembly per request — memoized via {@code PageModelResolutionContext}.
 *
 * Grouped reads (target ≤ 5 per dashboard-overview-runtime-v1 §12):
 *   1. tenant registry  → TenantCatalog.findRegistryById        (header + identity)
 *   2. day-window stats → TenantDashboardStatsService.getStats  (kpi.sales/tickets)
 *   3. operations bundle (outlets + terminals + sellers, 3 calls grouped) → readiness + operations
 *   4. commercial bundle (games + draw channels, 2 calls grouped)         → commercial
 *
 * Alerts / kpi.openDraws / kpi.activeSessions / kpi.pendingApprovals are V1
 * placeholders — no tenant-wide aggregate query exists yet (TODO V2).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantAdminDashboardPayloadAssembler {

  private static final int PUBLIC_CONTENT_LIMIT = 5;

  private final TenantCatalog tenantCatalog;
  private final GameCatalog gameCatalog;
  private final DrawChannelCatalog drawChannelCatalog;
  private final QueryBus queryBus;
  private final PublicContentApi publicContentApi;

  public Payload assemble(TchRequestContext ctx) {
    if (ctx == null || ctx.tenantId() == null) {
      return Payload.empty();
    }

    TenantId tenantId = ctx.tenantId();

    // Use tenant timezone for business-date "today"
    ZoneId tz = ctx.tenantZoneId() != null ? ctx.tenantZoneId() : ZoneOffset.UTC;

    // Grouped reads — loaded once, shared across builders.
    TenantRegistryView registry = tenantCatalog.findRegistryById(tenantId).orElse(null);
    OperationsBundle ops = loadOperationsBundle(tenantId);
    CommercialBundle commercial = loadCommercialBundle(tenantId);

    Map<String, Object> header         = buildHeader(ctx, registry);
    Map<String, Object> kpis           = buildKpis(ctx, ops, tz);
    Map<String, Object> readiness      = buildReadinessSummary(registry, ops, commercial);
    Map<String, Object> alerts         = buildAlerts();
    Map<String, Object> operations     = buildOperationsSummary(ops);
    Map<String, Object> commercialSummary = buildCommercialSummary(commercial);
    Map<String, Object> publicContent  = buildPublicContent();
    Map<String, Object> quickActions   = buildQuickActions();

    return new Payload(header, kpis, readiness, alerts, operations,
        commercialSummary, publicContent, quickActions);
  }

  // ---------------------- grouped bundle loaders ----------------------

  private OperationsBundle loadOperationsBundle(TenantId tenantId) {
    List<OutletView> outlets;
    try {
      outlets = queryBus.ask(new ListOutletsByTenantQuery(tenantId));
    } catch (RuntimeException e) {
      outlets = List.of();
    }

    long terminalCount;
    try {
      TchPage<TerminalSummaryView> page = queryBus.ask(new ListTerminalsQuery(
          TerminalSearchCriteria.empty(),
          new TchPageRequest(PageRequest.of(0, 1))));
      terminalCount = page != null ? page.totalElements() : 0L;
    } catch (RuntimeException e) {
      terminalCount = 0L;
    }

    List<SellerSummaryView> sellers;
    try {
      sellers = queryBus.ask(new ListSellersQuery(tenantId));
    } catch (RuntimeException e) {
      sellers = List.of();
    }

    return new OperationsBundle(outlets, terminalCount, sellers);
  }

  private CommercialBundle loadCommercialBundle(TenantId tenantId) {
    List<GameView> games;
    try {
      games = gameCatalog.listActive();
    } catch (RuntimeException e) {
      games = List.of();
    }

    List<DrawChannelSummaryView> channels;
    try {
      channels = drawChannelCatalog.listAll(tenantId, Boolean.TRUE);
    } catch (RuntimeException e) {
      channels = List.of();
    }

    return new CommercialBundle(games, channels);
  }

  // ---------------------- per-widget builders ----------------------

  private Map<String, Object> buildHeader(TchRequestContext ctx, TenantRegistryView registry) {
    return Map.of(
        "tenantCode", ctx.effectiveTenantCode() != null ? ctx.effectiveTenantCode() : "",
        "tenantId", ctx.tenantId() != null ? ctx.tenantId().value().toString() : "",
        "tenantName", registry != null && registry.name() != null ? registry.name() : "",
        "tenantStatus", registry != null && registry.status() != null ? registry.status().name() : "UNKNOWN",
        "tenantType", registry != null && registry.type() != null ? registry.type().name() : "UNKNOWN",
        "timezone", ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : "UTC",
        "currency", ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "");
  }

  private Map<String, Object> buildKpis(TchRequestContext ctx, OperationsBundle ops, ZoneId tz) {
    LocalDate today     = LocalDate.now(tz);
    LocalDate yesterday = today.minusDays(1);

    BigDecimal salesToday     = BigDecimal.ZERO;
    BigDecimal salesYesterday = BigDecimal.ZERO;
    long ticketsToday         = 0L;
    long ticketsYesterday     = 0L;
    BigDecimal winningsToday  = BigDecimal.ZERO;
    BigDecimal payoutsToday   = BigDecimal.ZERO;
    BigDecimal netToday       = BigDecimal.ZERO;

    try {
      // One query covering yesterday+today — split by refDate from dailyBreakdown
      TenantDashboardStatsView view = queryBus.ask(
          new GetTenantDashboardStatsQuery(ctx.tenantId(), yesterday, today, 5));
      if (view != null) {
        // today's row
        var todayRow = view.dailyBreakdown() == null ? null :
            view.dailyBreakdown().stream().filter(p -> today.equals(p.refDate())).findFirst().orElse(null);
        if (todayRow != null) {
          salesToday   = todayRow.grossSales() != null ? todayRow.grossSales() : BigDecimal.ZERO;
          ticketsToday = todayRow.ticketsSold();
        }
        // yesterday's row
        var yestRow = view.dailyBreakdown() == null ? null :
            view.dailyBreakdown().stream().filter(p -> yesterday.equals(p.refDate())).findFirst().orElse(null);
        if (yestRow != null) {
          salesYesterday   = yestRow.grossSales() != null ? yestRow.grossSales() : BigDecimal.ZERO;
          ticketsYesterday = yestRow.ticketsSold();
        }
        // summary-level for winnings / payouts / net (full window, use today row for single-day)
        if (view.summary() != null) {
          winningsToday = view.summary().winningsCalculated() != null
              ? view.summary().winningsCalculated() : BigDecimal.ZERO;
          payoutsToday  = view.summary().payoutsPaid() != null
              ? view.summary().payoutsPaid() : BigDecimal.ZERO;
          netToday      = view.summary().netRevenueEstimated() != null
              ? view.summary().netRevenueEstimated() : BigDecimal.ZERO;
        }
      }
    } catch (RuntimeException e) {
      log.warn("tenant_admin_dashboard: failed to load KPI stats — {}", e.getMessage());
    }

    return Map.ofEntries(
        Map.entry("salesToday",            salesToday),
        Map.entry("salesYesterday",        salesYesterday),
        Map.entry("ticketCountToday",      ticketsToday),
        Map.entry("ticketCountYesterday",  ticketsYesterday),
        Map.entry("winningsToday",         winningsToday),
        Map.entry("payoutsToday",          payoutsToday),
        Map.entry("netRevenueToday",       netToday),
        // V1 placeholders — no tenant-wide session/draw query yet
        Map.entry("activeSessions",        0L),
        Map.entry("openDraws",             0L),
        Map.entry("pendingApprovals",      0L));
  }

  /**
   * Operational readiness — derived from the loaded bundles.
   * READY when identity present + outlets + terminals + sellers + (games OR channels) configured.
   * PARTIAL if at least one configured. MISSING if none. UNKNOWN if no tenant.
   */
  private Map<String, Object> buildReadinessSummary(
      TenantRegistryView registry, OperationsBundle ops, CommercialBundle commercial) {
    List<TenantReadinessIssue> issues = new ArrayList<>();
    int missing = 0;

    if (registry == null) {
      issues.add(new TenantReadinessIssue("identity", "readiness.identity.missing", "/app/admin"));
      missing++;
    }
    if (ops.outlets().isEmpty()) {
      issues.add(new TenantReadinessIssue("outlets", "readiness.outlets.empty", "/app/admin/outlets"));
      missing++;
    }
    if (ops.terminalCount() == 0L) {
      issues.add(new TenantReadinessIssue("terminals", "readiness.terminals.empty", "/app/admin/terminals"));
      missing++;
    }
    if (ops.sellers().isEmpty()) {
      issues.add(new TenantReadinessIssue("sellers", "readiness.sellers.empty", "/app/admin/users"));
      missing++;
    }
    if (commercial.games().isEmpty()) {
      issues.add(new TenantReadinessIssue("games_pricing", "readiness.games.empty", "/app/admin/games-pricing"));
      missing++;
    }
    if (commercial.channels().isEmpty()) {
      issues.add(new TenantReadinessIssue("draws", "readiness.channels.empty", "/app/admin/draws"));
      missing++;
    }

    TenantReadinessStatus status;
    if (missing == 0) status = TenantReadinessStatus.READY;
    else if (missing >= 6) status = TenantReadinessStatus.MISSING;
    else status = TenantReadinessStatus.PARTIAL;

    TenantReadinessSummary summary = new TenantReadinessSummary(
        status, missing, issues.stream().limit(4).toList());

    return Map.of(
        "status", summary.status().name(),
        "missingCount", summary.missingCount(),
        "topIssues", summary.topIssues());
  }

  /**
   * V1 placeholder: no tenant-wide alerts/notification aggregate query exists yet.
   * Will be wired in V2 via GetTenantAlertsSummaryQuery (core.notification or equivalent).
   */
  private Map<String, Object> buildAlerts() {
    return Map.of(
        "unreadCount", 0,
        "topWarnings", List.of());
  }

  private Map<String, Object> buildOperationsSummary(OperationsBundle ops) {
    return Map.of(
        "users",
            Map.of("status", ops.sellers().isEmpty() ? "MISSING" : "READY", "count", ops.sellers().size()),
        "outlets",
            Map.of("status", ops.outlets().isEmpty() ? "MISSING" : "READY", "count", ops.outlets().size()),
        "terminals",
            Map.of("status", ops.terminalCount() == 0L ? "MISSING" : "READY", "count", ops.terminalCount()),
        "sessions",
            Map.of("status", "UNKNOWN", "count", 0));
  }

  private Map<String, Object> buildCommercialSummary(CommercialBundle commercial) {
    return Map.of(
        "gamesPricing",
            Map.of("status", commercial.games().isEmpty() ? "MISSING" : "READY",
                "count", commercial.games().size()),
        "drawChannels",
            Map.of("status", commercial.channels().isEmpty() ? "MISSING" : "READY",
                "count", commercial.channels().size()),
        "limits", Map.of("status", "UNKNOWN"),
        "promotions", Map.of("status", "UNKNOWN"));
  }

  private Map<String, Object> buildPublicContent() {
    try {
      List<PublicContentItemView> items =
          publicContentApi.listTenantAdminDashboardNews(PUBLIC_CONTENT_LIMIT);
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
      log.warn("tenant_admin_dashboard: could not load public content — {}", e.getMessage());
      return Map.of("items", List.of(), "count", 0);
    }
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
      Map<String, Object> publicContent,
      Map<String, Object> quickActions) {

    public static Payload empty() {
      return new Payload(Map.of(), Map.of(), Map.of(), Map.of(),
          Map.of(), Map.of(), Map.of(), Map.of());
    }
  }

  /** Grouped operational data — loaded once, shared by readiness + kpis + operations builders. */
  record OperationsBundle(
      List<OutletView> outlets,
      long terminalCount,
      List<SellerSummaryView> sellers) {}

  /** Grouped commercial data — loaded once, shared by readiness + commercial builders. */
  record CommercialBundle(
      List<GameView> games,
      List<DrawChannelSummaryView> channels) {}
}
