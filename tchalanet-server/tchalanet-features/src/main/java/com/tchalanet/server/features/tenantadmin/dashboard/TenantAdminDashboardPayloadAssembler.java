package com.tchalanet.server.features.tenantadmin.dashboard;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.core.analytics.api.query.GetTenantKpisQuery;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalCommissionStatsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.core.seller.api.query.ListSellersQuery;
import com.tchalanet.server.core.seller.api.query.model.SellerSummaryView;
import com.tchalanet.server.features.pagemodel.contract.ActionItem;
import com.tchalanet.server.features.pagemodel.contract.AlertItem;
import com.tchalanet.server.features.pagemodel.contract.NewsItem;
import com.tchalanet.server.features.pagemodel.contract.PublicContentPayload;
import com.tchalanet.server.features.pagemodel.contract.QuickActionsPayload;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessIssue;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessStatus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSummary;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the grouped payload for source {@code tenant_admin_dashboard}.
 * One assembly per request — memoized via {@code PageModelResolutionContext}.
 * <p>
 * Grouped reads (target ≤ 5 per dashboard-overview-runtime-v1 §12):
 * 1. tenant registry  → TenantCatalog.findRegistryById        (header + identity)
 * 2. day-window stats → TenantDashboardStatsService.getStats  (kpi.sales/tickets)
 * 3. operations bundle (seller terminals + sellers, 2 calls grouped)    → readiness + operations
 * 4. commercial bundle (games + draw channels, 2 calls grouped)         → commercial
 * <p>
 * Alerts / kpi.openDraws / kpi.activeSessions / kpi.pendingApprovals are V1
 * placeholders — no tenant-wide aggregate query exists yet (TODO V2).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantAdminDashboardPayloadAssembler {

    private static final int PUBLIC_CONTENT_LIMIT = 5;

    private final TenantPreContextLookupApi tenantPreContextLookupApi;
    private final GameCatalog gameCatalog;
    private final DrawChannelCatalog drawChannelCatalog;
    private final QueryBus queryBus;
    private final PublicContentApi publicContentApi;
    private final NotificationApi notificationApi;

    public Payload assemble(TchRequestContext ctx) {
        if (ctx == null || ctx.tenantId() == null) {
            return Payload.empty();
        }

        TenantId tenantId = ctx.tenantId();

        // Use tenant timezone for business-date "today"
        ZoneId tz = ctx.tenantZoneId() != null ? ctx.tenantZoneId() : ZoneOffset.UTC;

        // Grouped reads — loaded once, shared across builders.
        TenantContextLookupView registry = tenantPreContextLookupApi.findById(tenantId).orElse(null);
        OperationsBundle ops = loadOperationsBundle(tenantId);
        CommercialBundle commercial = loadCommercialBundle(tenantId);
        TenantKpisView kpisView = loadActiveSellerTerminals(tenantId, tz);
        long notifCount = loadNotificationCount(ctx);

        BigDecimal tenantDefaultRate = registry != null
            ? registry.defaultCommissionRate().orElse(null) : null;
        TenantCommissionSummaryPayload commission = loadCommissionSummary(tenantId, tenantDefaultRate);

        TenantDashboardHeaderPayload header = buildHeader(ctx, registry);
        TenantKpiGridPayload kpis = buildKpis(ctx, kpisView, notifCount, tz);
        TenantReadinessSummaryPayload readiness = buildReadinessSummary(registry, ops, commercial);
        TenantAlertsPayload alerts = buildAlerts(notifCount);
        TenantOperationsSummaryPayload operations = buildOperationsSummary(ops);
        TenantCommercialSummaryPayload commercialSummary = buildCommercialSummary(commercial);
        PublicContentPayload publicContent = buildPublicContent();
        QuickActionsPayload quickActions = buildQuickActions();

        return new Payload(header, kpis, readiness, alerts, operations,
            commercialSummary, commission, publicContent, quickActions);
    }

    // ---------------------- grouped bundle loaders ----------------------

    private OperationsBundle loadOperationsBundle(TenantId tenantId) {
        long sellerTerminalCount;
        try {
            TchPage<SellerTerminalSummaryRow> page = queryBus.ask(new ListSellerTerminalsQuery(
                tenantId,
                SellerTerminalSearchCriteria.empty(),
                new TchPageRequest(PageRequest.of(0, 1))));
            sellerTerminalCount = page != null ? page.totalElements() : 0L;
        } catch (RuntimeException e) {
            sellerTerminalCount = 0L;
        }

        List<SellerSummaryView> sellers;
        try {
            sellers = queryBus.ask(new ListSellersQuery(tenantId));
        } catch (RuntimeException e) {
            sellers = List.of();
        }

        return new OperationsBundle(sellerTerminalCount, sellers);
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

    private TenantKpisView loadActiveSellerTerminals(TenantId tenantId, ZoneId tz) {
        try {
            LocalDate today = LocalDate.now(tz);
            TenantKpisView view = queryBus.ask(new GetTenantKpisQuery(tenantId, today, today));
            return view != null ? view : TenantKpisView.empty();
        } catch (RuntimeException e) {
            log.warn("tenant_admin_dashboard: failed to load active seller terminals — {}", e.getMessage());
            return TenantKpisView.empty();
        }
    }

    private long loadNotificationCount(TchRequestContext ctx) {
        try {
            if (ctx.userId() == null) return 0L;
            String roleCode = ctx.currentRole() != null ? ctx.currentRole().name() : null;
            NotificationSummaryView summary = notificationApi.getNotificationSummary(
                new GetNotificationSummaryRequest(ctx.userId(), roleCode));
            return summary != null ? summary.unreadCount() : 0L;
        } catch (RuntimeException e) {
            log.warn("tenant_admin_dashboard: failed to load notification count — {}", e.getMessage());
            return 0L;
        }
    }

    // ---------------------- per-widget builders ----------------------

    private TenantDashboardHeaderPayload buildHeader(TchRequestContext ctx, TenantContextLookupView registry) {
        return new TenantDashboardHeaderPayload(
            ctx.effectiveTenantCode() != null ? ctx.effectiveTenantCode() : "",
            ctx.tenantId() != null ? ctx.tenantId().value().toString() : "",
            registry != null && registry.name() != null ? registry.name() : "",
            registry != null && registry.status() != null ? registry.status().name() : "UNKNOWN",
            registry != null && registry.type() != null ? registry.type().name() : "UNKNOWN",
            ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : "UTC",
            ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "");
    }

    private TenantKpiGridPayload buildKpis(TchRequestContext ctx, TenantKpisView kpisView, long notifCount, ZoneId tz) {
        LocalDate today = LocalDate.now(tz);
        LocalDate yesterday = today.minusDays(1);

        BigDecimal salesToday = BigDecimal.ZERO;
        BigDecimal salesYesterday = BigDecimal.ZERO;
        long ticketsToday = 0L;
        long ticketsYesterday = 0L;
        BigDecimal winningsToday = BigDecimal.ZERO;
        BigDecimal payoutsToday = BigDecimal.ZERO;
        BigDecimal netToday = BigDecimal.ZERO;

        try {
            // One query covering yesterday+today — split by refDate from dailyBreakdown
            TenantDashboardStatsView view = queryBus.ask(
                new GetTenantDashboardStatsQuery(ctx.tenantId(), yesterday, today, 5));
            if (view != null) {
                // today's row
                var todayRow = view.dailyBreakdown() == null ? null :
                    view.dailyBreakdown().stream().filter(p -> today.equals(p.refDate())).findFirst().orElse(null);
                if (todayRow != null) {
                    salesToday = todayRow.grossSales() != null ? todayRow.grossSales() : BigDecimal.ZERO;
                    ticketsToday = todayRow.ticketsSold();
                }
                // yesterday's row
                var yestRow = view.dailyBreakdown() == null ? null :
                    view.dailyBreakdown().stream().filter(p -> yesterday.equals(p.refDate())).findFirst().orElse(null);
                if (yestRow != null) {
                    salesYesterday = yestRow.grossSales() != null ? yestRow.grossSales() : BigDecimal.ZERO;
                    ticketsYesterday = yestRow.ticketsSold();
                }
                // summary-level for winnings / payouts / net (full window, use today row for single-day)
                if (view.summary() != null) {
                    winningsToday = view.summary().winningsCalculated() != null
                        ? view.summary().winningsCalculated() : BigDecimal.ZERO;
                    payoutsToday = view.summary().payoutsPaid() != null
                        ? view.summary().payoutsPaid() : BigDecimal.ZERO;
                    netToday = view.summary().netRevenueEstimated() != null
                        ? view.summary().netRevenueEstimated() : BigDecimal.ZERO;
                }
            }
        } catch (RuntimeException e) {
            log.warn("tenant_admin_dashboard: failed to load KPI stats — {}", e.getMessage());
        }

        long activeSellerTerminals = kpisView.activeCashiers(); // proxy V0: SELLER dim = seller_terminal
        return new TenantKpiGridPayload(
            salesToday, salesYesterday,
            ticketsToday, ticketsYesterday,
            winningsToday, payoutsToday, netToday,
            activeSellerTerminals, notifCount, 0L, 0L);
    }

    /**
     * Operational readiness — derived from the loaded bundles.
     * READY when identity present + outlets + terminals + sellers + (games OR channels) configured.
     * PARTIAL if at least one configured. MISSING if none. UNKNOWN if no tenant.
     */
    private TenantReadinessSummaryPayload buildReadinessSummary(
        TenantContextLookupView registry, OperationsBundle ops, CommercialBundle commercial) {
        List<TenantReadinessIssue> issues = new ArrayList<>();
        int missing = 0;

        if (registry == null) {
            issues.add(new TenantReadinessIssue("identity", "readiness.identity.missing", "/app/admin"));
            missing++;
        }
        if (ops.sellerTerminalCount() == 0L) {
            issues.add(new TenantReadinessIssue(
                "seller_terminals", "readiness.seller_terminals.empty", "/app/admin/seller-terminals"));
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

        return new TenantReadinessSummaryPayload(
            summary.status().name(), summary.missingCount(), summary.topIssues());
    }

    private TenantAlertsPayload buildAlerts(long notifCount) {
        return new TenantAlertsPayload((int) Math.min(notifCount, Integer.MAX_VALUE), List.of());
    }

    private TenantOperationsSummaryPayload buildOperationsSummary(OperationsBundle ops) {
        return new TenantOperationsSummaryPayload(
            new SectionStatus(ops.sellers().isEmpty() ? "MISSING" : "READY", ops.sellers().size()),
            new SectionStatus("PARKED", 0),
            new SectionStatus(ops.sellerTerminalCount() == 0L ? "MISSING" : "READY", ops.sellerTerminalCount()),
            new SectionStatus("UNKNOWN", 0));
    }

    private TenantCommercialSummaryPayload buildCommercialSummary(CommercialBundle commercial) {
        return new TenantCommercialSummaryPayload(
            new SectionStatus(commercial.games().isEmpty() ? "MISSING" : "READY", commercial.games().size()),
            new SectionStatus(commercial.channels().isEmpty() ? "MISSING" : "READY", commercial.channels().size()),
            new SectionStatus("UNKNOWN", 0),
            new SectionStatus("UNKNOWN", 0));
    }

    private PublicContentPayload buildPublicContent() {
        try {
            List<PublicContentItemView> items =
                publicContentApi.listTenantAdminDashboardNews(PUBLIC_CONTENT_LIMIT);
            List<NewsItem> news = items.stream()
                .map(i -> new NewsItem(
                    i.id() != null ? i.id().toString() : "",
                    i.title() != null ? i.title() : "",
                    i.content() != null ? i.content() : "",
                    i.sourceUrl() != null ? i.sourceUrl() : "",
                    "",
                    i.publishedAt() != null ? i.publishedAt().toString() : ""))
                .toList();
            return new PublicContentPayload(news, news.size());
        } catch (RuntimeException e) {
            log.warn("tenant_admin_dashboard: could not load public content — {}", e.getMessage());
            return PublicContentPayload.empty();
        }
    }

    private TenantCommissionSummaryPayload loadCommissionSummary(TenantId tenantId, BigDecimal tenantDefaultRate) {
        try {
            SellerTerminalCommissionStatsView stats = queryBus.ask(
                new GetSellerTerminalCommissionStatsQuery(tenantId, tenantDefaultRate));
            return new TenantCommissionSummaryPayload(
                tenantDefaultRate,
                stats.totalCount(),
                stats.countAtDefaultRate(),
                stats.countWithCustomRate(),
                stats.minRate(),
                stats.maxRate());
        } catch (RuntimeException e) {
            log.warn("tenant_admin_dashboard: failed to load commission stats — {}", e.getMessage());
            return TenantCommissionSummaryPayload.empty();
        }
    }

    private QuickActionsPayload buildQuickActions() {
        return new QuickActionsPayload(List.of(
            new ActionItem("ADD_SELLER_TERMINAL",    "quickaction.admin.add_seller_terminal",    "person_add",  "/app/admin/sellers/new"),
            new ActionItem("ACTIVE_SELLER_TERMINALS","quickaction.admin.active_seller_terminals","point_of_sale","/app/admin/sellers?status=active"),
            new ActionItem("DAILY_REPORT",           "quickaction.admin.daily_report",           "today",       "/app/admin/reports/today"),
            new ActionItem("MANAGE_LIMITS",          "quickaction.admin.manage_limits",          "shield",      "/app/admin/controls/limits"),
            new ActionItem("MANAGE_ODDS",            "quickaction.admin.manage_odds",            "percent",     "/app/admin/controls/odds"),
            new ActionItem("MARYAJ_GRATIS",          "quickaction.admin.maryaj_gratis",          "redeem",      "/app/admin/promotions/maryaj-gratis")));
    }

    public record Payload(
        TenantDashboardHeaderPayload header,
        TenantKpiGridPayload kpis,
        TenantReadinessSummaryPayload readiness,
        TenantAlertsPayload alerts,
        TenantOperationsSummaryPayload operations,
        TenantCommercialSummaryPayload commercial,
        TenantCommissionSummaryPayload commission,
        PublicContentPayload publicContent,
        QuickActionsPayload quickActions) {

        public static Payload empty() {
            return new Payload(
                new TenantDashboardHeaderPayload("", "", "", "UNKNOWN", "UNKNOWN", "UTC", ""),
                new TenantKpiGridPayload(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L, 0L),
                new TenantReadinessSummaryPayload("UNKNOWN", 0, List.of()),
                new TenantAlertsPayload(0, List.of()),
                new TenantOperationsSummaryPayload(
                    new SectionStatus("UNKNOWN", 0), new SectionStatus("UNKNOWN", 0),
                    new SectionStatus("UNKNOWN", 0), new SectionStatus("UNKNOWN", 0)),
                new TenantCommercialSummaryPayload(
                    new SectionStatus("UNKNOWN", 0), new SectionStatus("UNKNOWN", 0),
                    new SectionStatus("UNKNOWN", 0), new SectionStatus("UNKNOWN", 0)),
                TenantCommissionSummaryPayload.empty(),
                PublicContentPayload.empty(),
                QuickActionsPayload.empty());
        }
    }

    // ---- surface-specific typed payload records ----

    public record TenantDashboardHeaderPayload(
        String tenantCode,
        String tenantId,
        String tenantName,
        String tenantStatus,
        String tenantType,
        String timezone,
        String currency) {
    }

    public record TenantKpiGridPayload(
        BigDecimal salesToday,
        BigDecimal salesYesterday,
        long ticketCountToday,
        long ticketCountYesterday,
        BigDecimal winningsToday,
        BigDecimal payoutsToday,
        BigDecimal netRevenueToday,
        long activeSellerTerminals,  // V0 proxy: analytics_daily SELLER dim
        long notificationCount,
        long openDraws,
        long pendingApprovals) {
    }

    public record TenantReadinessSummaryPayload(
        String status,
        int missingCount,
        List<TenantReadinessIssue> topIssues) {
    }

    public record TenantAlertsPayload(
        int unreadCount,
        List<AlertItem> topWarnings) {
    }

    /**
     * Status + count for an operational/commercial section.
     */
    public record SectionStatus(String status, long count) {
    }

    public record TenantOperationsSummaryPayload(
        SectionStatus users,
        SectionStatus outlets,
        SectionStatus terminals,
        SectionStatus sessions) {
    }

    public record TenantCommercialSummaryPayload(
        SectionStatus gamesPricing,
        SectionStatus drawChannels,
        SectionStatus limits,
        SectionStatus promotions) {
    }

    /**
     * Commission configuration snapshot for the dashboard.
     * {@code tenantDefaultRate} is null when the tenant has not set a default.
     */
    public record TenantCommissionSummaryPayload(
        BigDecimal tenantDefaultRate,
        long totalSellerTerminals,
        long countAtDefaultRate,
        long countWithCustomRate,
        BigDecimal minRate,
        BigDecimal maxRate) {

        public static TenantCommissionSummaryPayload empty() {
            return new TenantCommissionSummaryPayload(null, 0L, 0L, 0L, null, null);
        }
    }

    /**
     * Grouped operational data — loaded once, shared by readiness + kpis + operations builders.
     */
    record OperationsBundle(
        long sellerTerminalCount,
        List<SellerSummaryView> sellers) {
    }

    /**
     * Grouped commercial data — loaded once, shared by readiness + commercial builders.
     */
    record CommercialBundle(
        List<GameView> games,
        List<DrawChannelSummaryView> channels) {
    }
}
