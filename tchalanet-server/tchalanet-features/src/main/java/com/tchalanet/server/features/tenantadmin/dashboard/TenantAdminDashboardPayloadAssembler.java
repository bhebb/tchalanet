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
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.core.outlet.api.query.ListOutletsByTenantQuery;
import com.tchalanet.server.core.outlet.api.query.OutletView;
import com.tchalanet.server.core.seller.api.query.ListSellersQuery;
import com.tchalanet.server.core.seller.api.query.model.SellerSummaryView;
import com.tchalanet.server.core.terminal.api.query.ListTerminalsQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
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
 * 3. operations bundle (outlets + terminals + sellers, 3 calls grouped) → readiness + operations
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

        TenantDashboardHeaderPayload header = buildHeader(ctx, registry);
        TenantKpiGridPayload kpis = buildKpis(ctx, ops, tz);
        TenantReadinessSummaryPayload readiness = buildReadinessSummary(registry, ops, commercial);
        TenantAlertsPayload alerts = buildAlerts();
        TenantOperationsSummaryPayload operations = buildOperationsSummary(ops);
        TenantCommercialSummaryPayload commercialSummary = buildCommercialSummary(commercial);
        PublicContentPayload publicContent = buildPublicContent();
        QuickActionsPayload quickActions = buildQuickActions();

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

    private TenantKpiGridPayload buildKpis(TchRequestContext ctx, OperationsBundle ops, ZoneId tz) {
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

        return new TenantKpiGridPayload(
            salesToday, salesYesterday,
            ticketsToday, ticketsYesterday,
            winningsToday, payoutsToday, netToday,
            0L, 0L, 0L); // activeSessions / openDraws / pendingApprovals: V1 placeholders
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

        return new TenantReadinessSummaryPayload(
            summary.status().name(), summary.missingCount(), summary.topIssues());
    }

    /**
     * V1 placeholder: no tenant-wide alerts/notification aggregate query exists yet.
     * Will be wired in V2 via GetTenantAlertsSummaryQuery (core.notification or equivalent).
     */
    private TenantAlertsPayload buildAlerts() {
        return new TenantAlertsPayload(0, List.of());
    }

    private TenantOperationsSummaryPayload buildOperationsSummary(OperationsBundle ops) {
        return new TenantOperationsSummaryPayload(
            new SectionStatus(ops.sellers().isEmpty() ? "MISSING" : "READY", ops.sellers().size()),
            new SectionStatus(ops.outlets().isEmpty() ? "MISSING" : "READY", ops.outlets().size()),
            new SectionStatus(ops.terminalCount() == 0L ? "MISSING" : "READY", ops.terminalCount()),
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

    private QuickActionsPayload buildQuickActions() {
        return new QuickActionsPayload(List.of(
            new ActionItem("CREATE_OUTLET", "quickaction.admin.create_outlet", "storefront", "/app/admin/outlets/new"),
            new ActionItem("CREATE_TERMINAL", "quickaction.admin.create_terminal", "point_of_sale", "/app/admin/terminals/new"),
            new ActionItem("CREATE_SELLER", "quickaction.admin.create_seller", "person_add", "/app/admin/users/new"),
            new ActionItem("TENANT_OVERVIEW", "quickaction.admin.overview", "map", "/app/admin/overview")));
    }

    public record Payload(
        TenantDashboardHeaderPayload header,
        TenantKpiGridPayload kpis,
        TenantReadinessSummaryPayload readiness,
        TenantAlertsPayload alerts,
        TenantOperationsSummaryPayload operations,
        TenantCommercialSummaryPayload commercial,
        PublicContentPayload publicContent,
        QuickActionsPayload quickActions) {

        public static Payload empty() {
            return new Payload(
                new TenantDashboardHeaderPayload("", "", "", "UNKNOWN", "UNKNOWN", "UTC", ""),
                new TenantKpiGridPayload(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L),
                new TenantReadinessSummaryPayload("UNKNOWN", 0, List.of()),
                new TenantAlertsPayload(0, List.of()),
                new TenantOperationsSummaryPayload(
                    new SectionStatus("UNKNOWN", 0), new SectionStatus("UNKNOWN", 0),
                    new SectionStatus("UNKNOWN", 0), new SectionStatus("UNKNOWN", 0)),
                new TenantCommercialSummaryPayload(
                    new SectionStatus("UNKNOWN", 0), new SectionStatus("UNKNOWN", 0),
                    new SectionStatus("UNKNOWN", 0), new SectionStatus("UNKNOWN", 0)),
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
        long activeSessions,
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
     * Grouped operational data — loaded once, shared by readiness + kpis + operations builders.
     */
    record OperationsBundle(
        List<OutletView> outlets,
        long terminalCount,
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
