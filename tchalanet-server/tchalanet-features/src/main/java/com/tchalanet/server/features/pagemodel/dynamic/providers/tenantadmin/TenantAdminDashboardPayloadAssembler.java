package com.tchalanet.server.features.pagemodel.dynamic.providers.tenantadmin;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustState;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustStateView;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import com.tchalanet.server.core.analytics.api.query.GetAnalyticsTrustStateQuery;
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.core.analytics.api.query.GetTenantFinancialBreakdownQuery;
import com.tchalanet.server.core.analytics.api.query.GetTenantKpisQuery;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.ListDrawsQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalCommissionStatsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.features.pagemodel.contract.ActionItem;
import com.tchalanet.server.features.pagemodel.contract.NewsItem;
import com.tchalanet.server.features.pagemodel.contract.PublicContentPayload;
import com.tchalanet.server.features.pagemodel.contract.QuickActionsPayload;
import com.tchalanet.server.features.pagemodel.dashboard.DashboardPeriod;
import com.tchalanet.server.features.shared.bff.BffSlicePolicy;
import com.tchalanet.server.features.shared.bff.BffSlices;
import com.tchalanet.server.features.tenantadmin.error.TenantAdminErrorCodes;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code tenant_admin_dashboard}. One assembly per request —
 * memoized via {@code PageModelResolutionContext}.
 *
 * <p>Grouped reads (target ≤ 5 per dashboard-overview-runtime-v1 §12): 1. tenant registry →
 * TenantCatalog.findRegistryById (header + identity) 2. day-window stats →
 * TenantDashboardStatsService.getStats (kpi.sales/tickets) 3. operations bundle (seller terminals +
 * sellers, 2 calls grouped) → readiness + operations 4. commercial bundle (games + draw channels, 2
 * calls grouped) → commercial
 *
 * <p>Tenant operational problems stay summarized here; detailed remediation belongs to the feature
 * pages linked from the widgets.
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
    return assemble(ctx, DashboardPeriod.TODAY, 0);
  }

  public Payload assemble(TchRequestContext ctx, DashboardPeriod period, int performancePage) {
    if (ctx == null || ctx.effectiveTenantIdOrNull() == null) {
      return Payload.empty();
    }

    TenantId tenantId = ctx.effectiveTenantIdRequired();

    // Use tenant timezone for business-date "today"
    ZoneId tz = ctx.tenantZoneId() != null ? ctx.tenantZoneId() : ZoneOffset.UTC;
    DashboardPeriod selectedPeriod = period != null ? period : DashboardPeriod.TODAY;
    DashboardPeriod.Windows windows = selectedPeriod.windows(LocalDate.now(tz));
    DashboardPeriod.Windows trendWindows = salesTrendWindows(windows);
    DashboardTiming timing = DashboardTiming.start();

    // Grouped reads — loaded once, shared across builders. A failed read becomes an explicit
    // unavailable section rather than a value that can be mistaken for a genuine zero.
    DashboardSlice<TenantContextLookupView> registry =
        timing.record(
            "registry",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_REGISTRY_UNAVAILABLE,
                    "tenant_admin_dashboard.registry",
                    () -> tenantPreContextLookupApi.findById(tenantId).orElse(null),
                    () -> null));
    DashboardSlice<OperationsBundle> ops =
        timing.record(
            "operations",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_OPERATIONS_UNAVAILABLE,
                    "tenant_admin_dashboard.operations",
                    () -> loadOperationsBundle(tenantId),
                    OperationsBundle::empty));
    DashboardSlice<CommercialBundle> commercial =
        timing.record(
            "commercial",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_COMMERCIAL_UNAVAILABLE,
                    "tenant_admin_dashboard.commercial",
                    () -> loadCommercialBundle(tenantId),
                    CommercialBundle::empty));
    DashboardSlice<AnalyticsTrustStateView> analyticsTrust =
        timing.record(
            "analyticsTrust",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_ANALYTICS_UNAVAILABLE,
                    "tenant_admin_dashboard.analytics",
                    () -> loadRequiredAnalyticsTrust(tenantId, windows),
                    () -> null));
    DashboardSlice<TenantKpisView> kpisView =
        timing.record(
            "kpis",
            () ->
                analyticsTrust.availability() == DashboardSectionAvailability.AVAILABLE
                    ? optionalSlice(
                        TenantAdminErrorCodes.DASHBOARD_KPIS_UNAVAILABLE,
                        "tenant_admin_dashboard.kpis",
                        () -> loadActiveSellerTerminals(tenantId, tz),
                        TenantKpisView::empty)
                    : DashboardSlice.unavailable(TenantKpisView.empty()));
    DashboardSlice<TenantDashboardStatsView> analytics =
        timing.record(
            "analytics",
            () ->
                analyticsTrust.availability() == DashboardSectionAvailability.AVAILABLE
                    ? optionalSlice(
                        TenantAdminErrorCodes.DASHBOARD_ANALYTICS_UNAVAILABLE,
                        "tenant_admin_dashboard.analytics",
                        () -> loadDashboardStats(tenantId, windows.from(), windows.to()),
                        () -> null)
                    : DashboardSlice.unavailable(null));
    DashboardSlice<AnalyticsTrustStateView> trendAnalyticsTrust =
        timing.record(
            "trendAnalyticsTrust",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_ANALYTICS_UNAVAILABLE,
                    "tenant_admin_dashboard.analytics.trend",
                    () -> loadRequiredAnalyticsTrust(tenantId, trendWindows),
                    () -> null));
    DashboardSlice<TenantDashboardStatsView> trendAnalytics =
        timing.record(
            "trendAnalytics",
            () ->
                trendAnalyticsTrust.availability() == DashboardSectionAvailability.AVAILABLE
                    ? optionalSlice(
                        TenantAdminErrorCodes.DASHBOARD_ANALYTICS_UNAVAILABLE,
                        "tenant_admin_dashboard.analytics.trend",
                        () -> loadDashboardStats(tenantId, trendWindows.from(), trendWindows.to()),
                        () -> null)
                    : DashboardSlice.unavailable(null));
    DashboardSlice<TenantDashboardStatsView> comparisonAnalytics =
        timing.record(
            "comparisonAnalytics",
            () ->
                analyticsTrust.availability() == DashboardSectionAvailability.AVAILABLE
                    ? optionalSlice(
                        TenantAdminErrorCodes.DASHBOARD_ANALYTICS_UNAVAILABLE,
                        "tenant_admin_dashboard.analytics.comparison",
                        () ->
                            loadDashboardStats(
                                tenantId, windows.comparisonFrom(), windows.comparisonTo()),
                        () -> null)
                    : DashboardSlice.unavailable(null));
    DashboardSlice<Long> openDraws =
        timing.record(
            "openDraws",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_OPEN_DRAWS_UNAVAILABLE,
                    "tenant_admin_dashboard.open_draws",
                    this::loadOpenDrawCount,
                    () -> 0L));
    DashboardSlice<Long> closedDraws =
        timing.record(
            "closedDraws",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_CLOSED_DRAWS_UNAVAILABLE,
                    "tenant_admin_dashboard.closed_draws",
                    () -> loadDrawCount(DrawStatus.CLOSED),
                    () -> 0L));
    DashboardSlice<Long> notifCount =
        timing.record(
            "notifications",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_NOTIFICATIONS_UNAVAILABLE,
                    "tenant_admin_dashboard.notifications",
                    () -> loadNotificationCount(ctx),
                    () -> 0L));

    BigDecimal tenantDefaultRate =
        registry.value() != null ? registry.value().defaultCommissionRate().orElse(null) : null;
    DashboardSlice<TenantCommissionSummaryPayload> commission =
        timing.record(
            "commission",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_COMMISSION_UNAVAILABLE,
                    "tenant_admin_dashboard.commission",
                    () -> loadCommissionSummary(tenantId, tenantDefaultRate),
                    TenantCommissionSummaryPayload::empty));

    TenantDashboardHeaderPayload header =
        timing.record("buildHeader", () -> buildHeader(ctx, registry.value()));
    TenantKpiGridPayload kpis =
        timing.record(
            "buildKpis",
            () ->
                buildKpis(
                    analytics.value(),
                    kpisView.value(),
                    openDraws.value(),
                    notifCount.value(),
                    tz));
    TenantOperationalKpiPayload operationalKpis =
        timing.record(
            "buildOperationalKpis",
            () ->
                buildOperationalKpis(
                    analytics.value(),
                    comparisonAnalytics.value(),
                    ops.value(),
                    selectedPeriod,
                    windows));
    TenantSalesTrendPayload salesTrend =
        timing.record(
            "buildSalesTrend", () -> buildSalesTrend(trendAnalytics.value(), trendWindows));
    DashboardSlice<TenantTerminalPerformancePayload> terminalPerformance =
        timing.record(
            "terminalPerformance",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_ANALYTICS_UNAVAILABLE,
                    "tenant_admin_dashboard.performance",
                    () ->
                        buildTerminalPerformance(
                            tenantId, windows.from(), windows.to(), performancePage, ops.value()),
                    TenantTerminalPerformancePayload::empty));
    TenantGameBreakdownPayload gameBreakdown =
        timing.record("buildGameBreakdown", () -> buildGameBreakdown(analytics.value()));
    TenantOperationsSummaryPayload operations =
        timing.record("buildOperations", () -> buildOperationsSummary(ops.value()));
    TenantCommercialSummaryPayload commercialSummary =
        timing.record("buildCommercial", () -> buildCommercialSummary(commercial.value()));
    DashboardSlice<PublicContentPayload> publicContent =
        timing.record(
            "publicContent",
            () ->
                optionalSlice(
                    TenantAdminErrorCodes.DASHBOARD_PUBLIC_CONTENT_UNAVAILABLE,
                    "tenant_admin_dashboard.public_content",
                    this::buildPublicContent,
                    PublicContentPayload::empty));
    QuickActionsPayload quickActions = timing.record("quickActions", this::buildQuickActions);

    Payload payload =
        new Payload(
            header,
            kpis,
            operationalKpis,
            terminalPerformance.value(),
            salesTrend,
            gameBreakdown,
            operations,
            commercialSummary,
            commission.value(),
            publicContent.value(),
            quickActions,
            sectionStates(
                registry,
                ops,
                commercial,
                kpisView,
                analytics,
                trendAnalytics,
                openDraws,
                closedDraws,
                notifCount,
                commission,
                publicContent));
    timing.logTenant(tenantId, ctx.effectiveTenantCode());
    return payload;
  }

  // ---------------------- grouped bundle loaders ----------------------

  private OperationsBundle loadOperationsBundle(TenantId tenantId) {
    TchPage<SellerTerminalSummaryRow> sellerTerminals =
        queryBus.ask(
            new ListSellerTerminalsQuery(
                tenantId,
                SellerTerminalSearchCriteria.empty(),
                new TchPageRequest(PageRequest.of(0, 500))));
    TchPage<SellerTerminalSummaryRow> activeSellerTerminals =
        queryBus.ask(
            new ListSellerTerminalsQuery(
                tenantId,
                new SellerTerminalSearchCriteria(null, SellerTerminalStatus.ACTIVE),
                new TchPageRequest(PageRequest.of(0, 1))));
    return new OperationsBundle(
        sellerTerminals != null ? sellerTerminals.totalElements() : 0L,
        sellerTerminals != null && sellerTerminals.items() != null
            ? sellerTerminals.items().stream()
                .filter(terminal -> SellerTerminalStatus.BLOCKED == terminal.status())
                .count()
            : 0L,
        activeSellerTerminals != null ? activeSellerTerminals.totalElements() : 0L,
        sellerTerminals != null && sellerTerminals.items() != null
            ? sellerTerminals.items()
            : List.of());
  }

  private CommercialBundle loadCommercialBundle(TenantId tenantId) {
    List<GameView> games = gameCatalog.listActive();
    List<DrawChannelSummaryView> channels = drawChannelCatalog.listAll(tenantId, Boolean.TRUE);
    ListLimitAssignmentsView assignments =
        queryBus.ask(new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.tenant(tenantId)));
    long enabledLimitCount =
        assignments != null && assignments.items() != null
            ? assignments.items().stream().filter(ListLimitAssignmentsView.Item::enabled).count()
            : 0L;

    return new CommercialBundle(
        games != null ? games : List.of(),
        channels != null ? channels : List.of(),
        enabledLimitCount);
  }

  private TenantKpisView loadActiveSellerTerminals(TenantId tenantId, ZoneId tz) {
    LocalDate today = LocalDate.now(tz);
    TenantKpisView view = queryBus.ask(new GetTenantKpisQuery(tenantId, today, today));
    return view != null ? view : TenantKpisView.empty();
  }

  private AnalyticsTrustStateView loadRequiredAnalyticsTrust(
      TenantId tenantId, DashboardPeriod.Windows windows) {
    AnalyticsTrustStateView trust =
        queryBus.ask(
            new GetAnalyticsTrustStateQuery(
                AnalyticsTrustScope.tenant(tenantId, windows.from(), windows.to())));
    if (trust == null || trust.state() != AnalyticsTrustState.READY) {
      throw new AnalyticsTrustUnavailableException();
    }
    return trust;
  }

  private TenantDashboardStatsView loadDashboardStats(
      TenantId tenantId, LocalDate from, LocalDate to) {
    return queryBus.ask(new GetTenantDashboardStatsQuery(tenantId, from, to, 5));
  }

  private long loadOpenDrawCount() {
    return loadDrawCount(DrawStatus.OPEN);
  }

  private long loadDrawCount(DrawStatus status) {
    DrawSearchCriteria criteria =
        new DrawSearchCriteria(null, status, null, null, null, null, null, null, null);
    TchPage<DrawSummary> page = queryBus.ask(new ListDrawsQuery(criteria, PageRequest.of(0, 1)));
    return page != null ? page.totalElements() : 0L;
  }

  private long loadNotificationCount(TchRequestContext ctx) {
    if (ctx.userId() == null) return 0L;
    String roleCode = ctx.currentRole() != null ? ctx.currentRole().name() : null;
    NotificationSummaryView summary =
        notificationApi.getNotificationSummary(
            new GetNotificationSummaryRequest(ctx.tenantId(), ctx.userId(), roleCode));
    return summary != null ? summary.unreadCount() : 0L;
  }

  private <T> DashboardSlice<T> optionalSlice(
      com.tchalanet.server.common.web.error.ErrorDescriptor descriptor,
      String target,
      Supplier<T> supplier,
      Supplier<T> fallback) {
    return BffSlices.optional(
        BffSlicePolicy.warn(
                descriptor.code(),
                "features.tenantadmin",
                NoticeSource.of("tenantAdminDashboard").operation(target),
                () -> DashboardSlice.unavailable(fallback.get()))
            .target(target),
        () -> DashboardSlice.available(supplier.get()));
  }

  private static TenantDashboardSectionStatesPayload sectionStates(
      DashboardSlice<?> registry,
      DashboardSlice<?> operations,
      DashboardSlice<?> commercial,
      DashboardSlice<?> kpis,
      DashboardSlice<?> analytics,
      DashboardSlice<?> trendAnalytics,
      DashboardSlice<?> openDraws,
      DashboardSlice<?> closedDraws,
      DashboardSlice<?> notifications,
      DashboardSlice<?> commission,
      DashboardSlice<?> publicContent) {
    return new TenantDashboardSectionStatesPayload(
        List.of(
            DashboardSectionState.of("registry", registry),
            DashboardSectionState.of("operations", operations),
            DashboardSectionState.of("commercial", commercial),
            DashboardSectionState.of("kpis", kpis),
            DashboardSectionState.of("analytics", analytics),
            DashboardSectionState.of("analyticsTrend", trendAnalytics),
            DashboardSectionState.of("openDraws", openDraws),
            DashboardSectionState.of("closedDraws", closedDraws),
            DashboardSectionState.of("notifications", notifications),
            DashboardSectionState.of("commission", commission),
            DashboardSectionState.of("publicContent", publicContent)));
  }

  // ---------------------- per-widget builders ----------------------

  private TenantDashboardHeaderPayload buildHeader(
      TchRequestContext ctx, TenantContextLookupView registry) {
    var tenantId = ctx.effectiveTenantIdOrNull();
    return new TenantDashboardHeaderPayload(
        ctx.effectiveTenantCode() != null ? ctx.effectiveTenantCode() : "",
        tenantId != null ? tenantId.value().toString() : "",
        registry != null && registry.displayName() != null ? registry.displayName() : "",
        registry != null && registry.status() != null ? registry.status().name() : "UNKNOWN",
        registry != null && registry.type() != null ? registry.type().name() : "UNKNOWN",
        ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : "UTC",
        ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "");
  }

  private TenantKpiGridPayload buildKpis(
      TenantDashboardStatsView view,
      TenantKpisView kpisView,
      long openDraws,
      long notifCount,
      ZoneId tz) {
    LocalDate today = LocalDate.now(tz);
    LocalDate yesterday = today.minusDays(1);

    BigDecimal salesToday = kpisView.totalSales() != null ? kpisView.totalSales() : BigDecimal.ZERO;
    BigDecimal salesYesterday = BigDecimal.ZERO;
    long ticketsToday = kpisView.ticketsSold();
    long ticketsYesterday = 0L;
    BigDecimal winningsToday =
        kpisView.totalPayout() != null ? kpisView.totalPayout() : BigDecimal.ZERO;
    BigDecimal payoutsToday =
        kpisView.totalPayout() != null ? kpisView.totalPayout() : BigDecimal.ZERO;
    BigDecimal netToday = kpisView.netRevenue() != null ? kpisView.netRevenue() : BigDecimal.ZERO;

    if (view != null) {
      var yestRow =
          view.dailyBreakdown() == null
              ? null
              : view.dailyBreakdown().stream()
                  .filter(p -> yesterday.equals(p.refDate()))
                  .findFirst()
                  .orElse(null);
      if (yestRow != null) {
        salesYesterday = yestRow.grossSales() != null ? yestRow.grossSales() : BigDecimal.ZERO;
        ticketsYesterday = yestRow.ticketsSold();
      }
    }

    long activeSellerTerminals =
        kpisView.activeCashiers(); // proxy V0: SELLER dim = seller_terminal
    return new TenantKpiGridPayload(
        salesToday,
        salesYesterday,
        ticketsToday,
        ticketsYesterday,
        winningsToday,
        payoutsToday,
        netToday,
        activeSellerTerminals,
        openDraws,
        notifCount,
        0L);
  }

  private TenantOperationalKpiPayload buildOperationalKpis(
      TenantDashboardStatsView current,
      TenantDashboardStatsView comparison,
      OperationsBundle operations,
      DashboardPeriod period,
      DashboardPeriod.Windows windows) {
    var currentSummary = current != null ? current.summary() : null;
    var comparisonSummary = comparison != null ? comparison.summary() : null;
    BigDecimal currentSales = money(currentSummary != null ? currentSummary.grossSales() : null);
    BigDecimal previousSales =
        money(comparisonSummary != null ? comparisonSummary.grossSales() : null);
    BigDecimal currentCommission =
        money(currentSummary != null ? currentSummary.sellerCommission() : null);
    BigDecimal previousCommission =
        money(comparisonSummary != null ? comparisonSummary.sellerCommission() : null);
    BigDecimal currentNet =
        money(currentSummary != null ? currentSummary.netRevenueEstimated() : null);
    BigDecimal previousNet =
        money(comparisonSummary != null ? comparisonSummary.netRevenueEstimated() : null);
    BigDecimal activePos =
        BigDecimal.valueOf(operations != null ? operations.activeSellerTerminalCount() : 0L);

    return new TenantOperationalKpiPayload(
        period.name(),
        periodComparisonName(period),
        "dashboard.period." + period.name().toLowerCase(),
        "dashboard.period." + periodComparisonName(period).toLowerCase(),
        windows.from().toString(),
        windows.to().toString(),
        metric(currentSales, previousSales),
        metric(currentCommission, previousCommission),
        metric(currentNet, previousNet),
        new DashboardMetric(activePos, BigDecimal.ZERO, BigDecimal.ZERO));
  }

  private TenantTerminalPerformancePayload buildTerminalPerformance(
      TenantId tenantId,
      LocalDate from,
      LocalDate to,
      int requestedPage,
      OperationsBundle operations) {
    TenantFinancialBreakdownView breakdown =
        queryBus.ask(
            new GetTenantFinancialBreakdownQuery(tenantId, from, to, 1, 500, List.of(), List.of()));
    Map<UUID, TerminalPerformanceAccumulator> aggregates = new HashMap<>();
    if (breakdown != null && breakdown.sellerTerminalDailyRows() != null) {
      for (var row : breakdown.sellerTerminalDailyRows()) {
        var accumulator =
            aggregates.computeIfAbsent(
                row.sellerTerminalId(), ignored -> new TerminalPerformanceAccumulator());
        accumulator.ticketsSold += row.ticketsSold();
        accumulator.grossSales = accumulator.grossSales.add(money(row.grossSales()));
        accumulator.sellerCommission =
            accumulator.sellerCommission.add(money(row.sellerCommission()));
        accumulator.netRevenue = accumulator.netRevenue.add(money(row.netRevenueEstimated()));
      }
    }

    Map<UUID, String> terminalNames = new HashMap<>();
    if (operations != null && operations.terminals() != null) {
      for (var terminal : operations.terminals()) {
        if (terminal.id() != null && terminal.id().value() != null) {
          terminalNames.put(
              terminal.id().value(),
              terminal.displayName() != null && !terminal.displayName().isBlank()
                  ? terminal.displayName()
                  : terminal.terminalCode());
        }
      }
    }

    List<TenantTerminalPerformanceRow> rows =
        aggregates.entrySet().stream()
            .map(
                entry -> {
                  UUID id = entry.getKey();
                  var value = entry.getValue();
                  return new TenantTerminalPerformanceRow(
                      id.toString(),
                      terminalNames.getOrDefault(id, id.toString()),
                      value.ticketsSold,
                      value.grossSales,
                      value.sellerCommission,
                      value.netRevenue);
                })
            .sorted(
                Comparator.comparing(TenantTerminalPerformanceRow::grossSales)
                    .reversed()
                    .thenComparing(TenantTerminalPerformanceRow::label))
            .toList();
    int pageSize = 15;
    int page = Math.max(0, requestedPage);
    int fromIndex = Math.min(page * pageSize, rows.size());
    int toIndex = Math.min(fromIndex + pageSize, rows.size());
    return new TenantTerminalPerformancePayload(
        page, pageSize, rows.size(), rows.subList(fromIndex, toIndex));
  }

  private static DashboardMetric metric(BigDecimal value, BigDecimal comparison) {
    BigDecimal current = money(value);
    BigDecimal previous = money(comparison);
    BigDecimal delta = current.subtract(previous);
    BigDecimal deltaPercent =
        previous.signum() == 0
            ? (current.signum() == 0 ? BigDecimal.ZERO : null)
            : delta.divide(previous, 4, java.math.RoundingMode.HALF_UP).movePointRight(2);
    return new DashboardMetric(current, delta, deltaPercent);
  }

  private static BigDecimal money(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static String periodComparisonName(DashboardPeriod period) {
    return switch (period) {
      case TODAY -> "YESTERDAY";
      case YESTERDAY -> "PREVIOUS_DAY";
      case THIS_WEEK -> "PREVIOUS_WEEK";
      case LAST_WEEK -> "PREVIOUS_WEEK";
    };
  }

  private TenantSalesTrendPayload buildSalesTrend(
      TenantDashboardStatsView view, DashboardPeriod.Windows windows) {
    if (view == null || view.dailyBreakdown() == null || view.dailyBreakdown().isEmpty()) {
      return new TenantSalesTrendPayload(List.of());
    }

    Map<LocalDate, TenantDashboardStatsView.TenantDailyPoint> byDate =
        view.dailyBreakdown().stream()
            .filter(point -> point.refDate() != null)
            .collect(
                Collectors.toMap(
                    TenantDashboardStatsView.TenantDailyPoint::refDate,
                    point -> point,
                    (first, ignored) -> first,
                    LinkedHashMap::new));

    List<TenantTrendItem> points = new ArrayList<>();
    for (LocalDate date = windows.from(); !date.isAfter(windows.to()); date = date.plusDays(1)) {
      var point = byDate.get(date);
      points.add(
          new TenantTrendItem(
              date.toString(),
              date.toString(),
              point != null && point.grossSales() != null ? point.grossSales() : BigDecimal.ZERO,
              point != null && point.netRevenueEstimated() != null
                  ? point.netRevenueEstimated()
                  : BigDecimal.ZERO,
              point != null ? point.ticketsSold() : 0L));
    }

    return new TenantSalesTrendPayload(points);
  }

  /** The chart is a seven-day view anchored to the selected period's upper bound. */
  private static DashboardPeriod.Windows salesTrendWindows(DashboardPeriod.Windows selected) {
    LocalDate to = selected.to();
    LocalDate from = to.minusDays(6);
    return new DashboardPeriod.Windows(from, to, from.minusDays(7), to.minusDays(7));
  }

  private TenantGameBreakdownPayload buildGameBreakdown(TenantDashboardStatsView view) {
    if (view == null || view.gameBreakdown() == null) {
      return new TenantGameBreakdownPayload(List.of());
    }
    List<TenantGameBreakdownItem> items =
        view.gameBreakdown().stream()
            .map(
                g ->
                    new TenantGameBreakdownItem(
                        g.gameCode() != null ? g.gameCode() : "",
                        g.gameLabel() != null ? g.gameLabel() : g.gameCode(),
                        g.ticketsSold(),
                        g.grossSales() != null ? g.grossSales() : BigDecimal.ZERO,
                        g.netRevenueEstimated() != null
                            ? g.netRevenueEstimated()
                            : BigDecimal.ZERO))
            .toList();
    return new TenantGameBreakdownPayload(items);
  }

  private TenantOperationsSummaryPayload buildOperationsSummary(OperationsBundle ops) {
    return new TenantOperationsSummaryPayload(
        new SectionStatus(ops.sellerTerminalCount() == 0L ? "MISSING" : "READY", 0L),
        new SectionStatus("PARKED", 0),
        new SectionStatus(
            ops.sellerTerminalCount() == 0L ? "MISSING" : "READY", ops.sellerTerminalCount()),
        new SectionStatus("UNKNOWN", 0));
  }

  private TenantCommercialSummaryPayload buildCommercialSummary(CommercialBundle commercial) {
    return new TenantCommercialSummaryPayload(
        new SectionStatus(
            commercial.games().isEmpty() ? "MISSING" : "READY", commercial.games().size()),
        new SectionStatus(
            commercial.channels().isEmpty() ? "MISSING" : "READY", commercial.channels().size()),
        new SectionStatus("UNKNOWN", 0),
        new SectionStatus("UNKNOWN", 0));
  }

  private PublicContentPayload buildPublicContent() {
    List<PublicContentItemView> items =
        publicContentApi.listTenantAdminDashboardNews(PUBLIC_CONTENT_LIMIT);
    List<NewsItem> news =
        (items == null ? List.<PublicContentItemView>of() : items)
            .stream()
                .map(
                    i ->
                        new NewsItem(
                            i.id() != null ? i.id().toString() : "",
                            i.title() != null ? i.title() : "",
                            i.content() != null ? i.content() : "",
                            i.sourceUrl() != null ? i.sourceUrl() : "",
                            "",
                            i.publishedAt() != null ? i.publishedAt().toString() : ""))
                .toList();
    return new PublicContentPayload(news, news.size());
  }

  private TenantCommissionSummaryPayload loadCommissionSummary(
      TenantId tenantId, BigDecimal tenantDefaultRate) {
    SellerTerminalCommissionStatsView stats =
        queryBus.ask(new GetSellerTerminalCommissionStatsQuery(tenantId, tenantDefaultRate));
    if (stats == null) {
      return TenantCommissionSummaryPayload.empty();
    }
    return new TenantCommissionSummaryPayload(
        tenantDefaultRate,
        stats.totalCount(),
        stats.countAtDefaultRate(),
        stats.countWithCustomRate(),
        stats.minRate(),
        stats.maxRate());
  }

  private QuickActionsPayload buildQuickActions() {
    return new QuickActionsPayload(
        List.of(
            new ActionItem(
                "TODAYS_DRAWS",
                "quickaction.admin.todays_draws",
                "event_available",
                "/app/admin/draws?date=TODAY"),
            new ActionItem(
                "DAILY_REPORT",
                "quickaction.admin.daily_report",
                "today",
                "/app/admin/reports/daily"),
            new ActionItem(
                "SELLER_TERMINALS",
                "quickaction.admin.seller_terminals",
                "point_of_sale",
                "/app/admin/seller-terminals"),
            new ActionItem(
                "SALES_BY_TERMINAL",
                "quickaction.admin.sales_by_terminal",
                "receipt_long",
                "/app/admin/reports/sellers"),
            new ActionItem(
                "SALES_BY_DRAW",
                "quickaction.admin.sales_by_draw",
                "analytics",
                "/app/admin/reports/draws"),
            new ActionItem(
                "BLOCK_NUMBER",
                "quickaction.admin.block_number",
                "block",
                "/app/admin/limits/number"),
            new ActionItem(
                "BLOCK_SELLER",
                "quickaction.admin.block_seller",
                "phonelink_lock",
                "/app/admin/seller-terminals"),
            new ActionItem(
                "RESET_SELLER_PIN",
                "quickaction.admin.reset_seller_pin",
                "lock_reset",
                "/app/admin/seller-terminals"),
            new ActionItem(
                "DRAW_RESULTS",
                "quickaction.admin.draw_results",
                "fact_check",
                "/app/admin/draws/results")));
  }

  public record Payload(
      TenantDashboardHeaderPayload header,
      TenantKpiGridPayload kpis,
      TenantOperationalKpiPayload operationalKpis,
      TenantTerminalPerformancePayload terminalPerformance,
      TenantSalesTrendPayload salesTrend,
      TenantGameBreakdownPayload gameBreakdown,
      TenantOperationsSummaryPayload operations,
      TenantCommercialSummaryPayload commercial,
      TenantCommissionSummaryPayload commission,
      PublicContentPayload publicContent,
      QuickActionsPayload quickActions,
      TenantDashboardSectionStatesPayload sectionStates) {

    public static Payload empty() {
      return new Payload(
          new TenantDashboardHeaderPayload("", "", "", "UNKNOWN", "UNKNOWN", "UTC", ""),
          new TenantKpiGridPayload(
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              0L,
              0L,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              0L,
              0L,
              0L,
              0L),
          TenantOperationalKpiPayload.empty(),
          TenantTerminalPerformancePayload.empty(),
          new TenantSalesTrendPayload(List.of()),
          new TenantGameBreakdownPayload(List.of()),
          new TenantOperationsSummaryPayload(
              new SectionStatus("UNKNOWN", 0),
              new SectionStatus("UNKNOWN", 0),
              new SectionStatus("UNKNOWN", 0),
              new SectionStatus("UNKNOWN", 0)),
          new TenantCommercialSummaryPayload(
              new SectionStatus("UNKNOWN", 0),
              new SectionStatus("UNKNOWN", 0),
              new SectionStatus("UNKNOWN", 0),
              new SectionStatus("UNKNOWN", 0)),
          TenantCommissionSummaryPayload.empty(),
          PublicContentPayload.empty(),
          QuickActionsPayload.empty(),
          TenantDashboardSectionStatesPayload.unknown());
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
      String currency) {}

  public record TenantKpiGridPayload(
      BigDecimal salesToday,
      BigDecimal salesYesterday,
      long ticketCountToday,
      long ticketCountYesterday,
      BigDecimal winningsToday,
      BigDecimal payoutsToday,
      BigDecimal netRevenueToday,
      long activeSellerTerminals,
      // V0 proxy: analytics_daily SELLER dim
      long openDraws,
      long notificationCount,
      long pendingApprovals) {}

  public record TenantOperationalKpiPayload(
      String period,
      String comparisonPeriod,
      String periodLabelKey,
      String comparisonLabelKey,
      String fromDate,
      String toDate,
      DashboardMetric grossSales,
      DashboardMetric sellerCommissionPayable,
      DashboardMetric estimatedNetRevenue,
      DashboardMetric availablePos) {

    public static TenantOperationalKpiPayload empty() {
      var empty = new DashboardMetric(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
      return new TenantOperationalKpiPayload(
          DashboardPeriod.TODAY.name(),
          "YESTERDAY",
          "dashboard.period.today",
          "dashboard.period.yesterday",
          "",
          "",
          empty,
          empty,
          empty,
          empty);
    }
  }

  public record DashboardMetric(BigDecimal value, BigDecimal delta, BigDecimal deltaPercent) {}

  public record TenantTerminalPerformancePayload(
      int page, int pageSize, long totalElements, List<TenantTerminalPerformanceRow> rows) {

    public static TenantTerminalPerformancePayload empty() {
      return new TenantTerminalPerformancePayload(0, 15, 0L, List.of());
    }
  }

  public record TenantTerminalPerformanceRow(
      String id,
      String label,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal sellerCommission,
      BigDecimal netRevenueEstimated) {}

  public record TenantSalesTrendPayload(List<TenantTrendItem> points) {}

  public record TenantTrendItem(
      String id, String label, BigDecimal grossSales, BigDecimal netRevenue, long ticketsSold) {}

  public record TenantGameBreakdownPayload(List<TenantGameBreakdownItem> items) {}

  public record TenantGameBreakdownItem(
      String gameCode,
      String label,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal netRevenue) {}

  /** Explicit state for every independently loaded dashboard section. */
  public record TenantDashboardSectionStatesPayload(List<DashboardSectionState> items) {

    public static TenantDashboardSectionStatesPayload unknown() {
      return new TenantDashboardSectionStatesPayload(List.of());
    }
  }

  public record DashboardSectionState(String id, String status) {

    private static DashboardSectionState of(String id, DashboardSlice<?> slice) {
      return new DashboardSectionState(id, slice.availability().name());
    }
  }

  /** Status + count for an operational/commercial section. */
  public record SectionStatus(String status, long count) {}

  public record TenantOperationsSummaryPayload(
      SectionStatus users,
      SectionStatus outlets,
      SectionStatus terminals,
      SectionStatus sessions) {}

  public record TenantCommercialSummaryPayload(
      SectionStatus gamesPricing,
      SectionStatus drawChannels,
      SectionStatus limits,
      SectionStatus promotions) {}

  /**
   * Commission configuration snapshot for the dashboard. {@code tenantDefaultRate} is null when the
   * tenant has not set a default.
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

  /** Grouped operational data — loaded once, shared by readiness + kpis + operations builders. */
  record OperationsBundle(
      long sellerTerminalCount,
      long blockedSellerTerminalCount,
      long activeSellerTerminalCount,
      List<SellerTerminalSummaryRow> terminals) {

    static OperationsBundle empty() {
      return new OperationsBundle(0L, 0L, 0L, List.of());
    }
  }

  private static final class TerminalPerformanceAccumulator {
    private long ticketsSold;
    private BigDecimal grossSales = BigDecimal.ZERO;
    private BigDecimal sellerCommission = BigDecimal.ZERO;
    private BigDecimal netRevenue = BigDecimal.ZERO;
  }

  /** Grouped commercial data — loaded once, shared by readiness + commercial builders. */
  record CommercialBundle(
      List<GameView> games, List<DrawChannelSummaryView> channels, long enabledLimitCount) {

    static CommercialBundle empty() {
      return new CommercialBundle(List.of(), List.of(), 0L);
    }
  }

  private record DashboardSlice<T>(T value, DashboardSectionAvailability availability) {

    private static <T> DashboardSlice<T> available(T value) {
      return new DashboardSlice<>(
          value,
          value == null
              ? DashboardSectionAvailability.EMPTY
              : DashboardSectionAvailability.AVAILABLE);
    }

    private static <T> DashboardSlice<T> unavailable(T value) {
      return new DashboardSlice<>(value, DashboardSectionAvailability.UNAVAILABLE);
    }
  }

  private enum DashboardSectionAvailability {
    AVAILABLE,
    EMPTY,
    UNAVAILABLE
  }

  private static final class DashboardTiming {
    private final long startedAt = System.nanoTime();
    private final Map<String, Long> durationsMs = new LinkedHashMap<>();

    static DashboardTiming start() {
      return new DashboardTiming();
    }

    <T> T record(String name, Supplier<T> supplier) {
      long before = System.nanoTime();
      try {
        return supplier.get();
      } finally {
        durationsMs.put(name + "Ms", elapsedMs(before));
      }
    }

    void logTenant(TenantId tenantId, String tenantCode) {
      log.warn(
          "dashboard_timing surface=tenant_admin totalMs={} tenantId={} tenantCode={} blocks={}",
          elapsedMs(startedAt),
          tenantId != null ? tenantId.value() : null,
          tenantCode,
          durationsMs);
    }

    private static long elapsedMs(long startedAt) {
      return (System.nanoTime() - startedAt) / 1_000_000L;
    }
  }

  /** Internal control flow only; BffSlices translates it to a stable degradation notice. */
  private static final class AnalyticsTrustUnavailableException extends RuntimeException {}
}
