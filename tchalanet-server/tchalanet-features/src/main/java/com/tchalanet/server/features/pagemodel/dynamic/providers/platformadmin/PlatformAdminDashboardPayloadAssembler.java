package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.shared.bff.BffSlicePolicy;
import com.tchalanet.server.features.shared.bff.BffSlices;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView;
import com.tchalanet.server.core.analytics.api.query.GetPlatformDashboardStatsQuery;
import com.tchalanet.server.core.subscription.api.query.GetPlatformSubscriptionStatsQuery;
import com.tchalanet.server.core.subscription.api.query.PlatformSubscriptionStatsView;
import com.tchalanet.server.features.pagemodel.contract.ActionItem;
import com.tchalanet.server.features.pagemodel.contract.NewsItem;
import com.tchalanet.server.features.pagemodel.contract.PublicContentPayload;
import com.tchalanet.server.features.pagemodel.contract.QuickActionsPayload;
import com.tchalanet.server.features.platformadmin.error.PlatformAdminErrorCodes;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatsView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Loads the commercial grouped payload for source {@code platform_admin_dashboard}. One assembly
 * per request — memoized via {@code PageModelResolutionContext}.
 *
 * <p>Commercial reads: 1. TenantCatalog.stats() (tenant counts) 2.
 * QueryBus.ask(GetPlatformDashboardStatsQuery) (sales KPIs, trends, top tenants) 3.
 * QueryBus.ask(GetPlatformSubscriptionStatsQuery) 4. TenantCatalog.listTenants(size=50)
 * (onboarding) 5. PublicContentApi (public content preview)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminDashboardPayloadAssembler {

  private static final int PUBLIC_CONTENT_LIMIT = 5;

  private final TenantPreContextLookupApi tenantPreContextLookupApi;
  private final QueryBus queryBus;
  private final PublicContentApi publicContentApi;

  public Payload assemble(TchRequestContext ctx) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    DashboardTiming timing = DashboardTiming.start();

    TenantStatsView tenantStats =
        timing.record(
            "tenantStats",
            () ->
                optionalSlice(
                    PlatformAdminErrorCodes.DASHBOARD_TENANTS_UNAVAILABLE,
                    "platform_admin_dashboard.tenants",
                    tenantPreContextLookupApi::stats,
                    () -> new TenantStatsView(0, 0, 0)));
    LocalDate from = today.minusDays(6);
    PlatformDashboardStatsView analytics =
        timing.record(
            "analytics",
            () ->
                optionalSlice(
                    PlatformAdminErrorCodes.DASHBOARD_ANALYTICS_UNAVAILABLE,
                    "platform_admin_dashboard.analytics",
                    () -> queryBus.ask(new GetPlatformDashboardStatsQuery(from, today, 5, 5)),
                    () -> null));
    TenantKpiSummaryPayload tenants =
        timing.record("tenantsKpi", () -> buildTenantsKpi(tenantStats, analytics, today));
    PlatformSalesPayload sales = timing.record("sales", () -> buildPlatformSales(tenants));
    TenantRankingPayload tenantRanking =
        timing.record("tenantRanking", () -> buildTenantRanking(tenants));
    SubscriptionSummaryPayload subscriptions =
        timing.record(
            "subscriptions",
            () ->
                optionalSlice(
                    PlatformAdminErrorCodes.DASHBOARD_SUBSCRIPTIONS_UNAVAILABLE,
                    "platform_admin_dashboard.subscriptions",
                    this::buildSubscriptions,
                    SubscriptionSummaryPayload::empty));
    OnboardingAlertsPayload onboardingAlerts =
        timing.record(
            "onboardingAlerts",
            () ->
                optionalSlice(
                    PlatformAdminErrorCodes.DASHBOARD_ONBOARDING_UNAVAILABLE,
                    "platform_admin_dashboard.onboarding",
                    this::buildOnboardingAlerts,
                    OnboardingAlertsPayload::empty));
    PublicContentPayload publicContent =
        timing.record(
            "publicContent",
            () ->
                optionalSlice(
                    PlatformAdminErrorCodes.DASHBOARD_PUBLIC_CONTENT_UNAVAILABLE,
                    "platform_admin_dashboard.public_content",
                    this::buildPublicContent,
                    PublicContentPayload::empty));
    QuickActionsPayload quickActions = timing.record("quickActions", this::buildQuickActions);

    Payload payload =
        new Payload(
            tenants,
            sales,
            tenantRanking,
            subscriptions,
            onboardingAlerts,
            publicContent,
            quickActions);
    timing.logPlatformCommercial(ctx);
    return payload;
  }

  /**
   * Tenant counts (from catalog) + analytics KPIs (tickets/sales today, top tenants). Two grouped
   * reads bundled in one widget section.
   */
  private TenantKpiSummaryPayload buildTenantsKpi(
      TenantStatsView catalogStats, PlatformDashboardStatsView statsView, LocalDate today) {
    long total = catalogStats != null ? catalogStats.total() : 0L;
    long active = catalogStats != null ? catalogStats.active() : 0L;
    long suspended = catalogStats != null ? catalogStats.suspended() : 0L;
    long onboarding = 0L;

    long ticketsSoldToday = 0L;
    BigDecimal grossSalesToday = BigDecimal.ZERO;
    BigDecimal netToday = BigDecimal.ZERO;
    List<DailyTrendItem> dailyBreakdown = List.of();
    List<GameBreakdownItem> gameBreakdown = List.of();
    List<TenantRankItem> topTenants = List.of();

    if (statsView != null && statsView.summary() != null) {
      if (total == 0L) total = statsView.summary().totalTenants();
      var todayPoint =
          statsView.dailyBreakdown() == null
              ? null
              : statsView.dailyBreakdown().stream()
                  .filter(p -> today.equals(p.refDate()))
                  .findFirst()
                  .orElse(null);
      if (todayPoint != null) {
        ticketsSoldToday = todayPoint.ticketsSold();
        grossSalesToday =
            todayPoint.grossSales() != null ? todayPoint.grossSales() : BigDecimal.ZERO;
        netToday =
            todayPoint.netRevenueEstimated() != null
                ? todayPoint.netRevenueEstimated()
                : BigDecimal.ZERO;
      }
      if (statsView.topTenants() != null) {
        topTenants =
            statsView.topTenants().stream()
                .map(
                    r ->
                        new TenantRankItem(
                            r.tenantCode() != null ? r.tenantCode() : "",
                            r.ticketsSold(),
                            r.grossSales() != null ? r.grossSales() : BigDecimal.ZERO,
                            r.netRevenueEstimated() != null
                                ? r.netRevenueEstimated()
                                : BigDecimal.ZERO))
                .toList();
      }
      if (statsView.dailyBreakdown() != null) {
        dailyBreakdown =
            statsView.dailyBreakdown().stream()
                .map(
                    p ->
                        new DailyTrendItem(
                            p.refDate() != null ? p.refDate().toString() : "",
                            p.ticketsSold(),
                            p.grossSales() != null ? p.grossSales() : BigDecimal.ZERO,
                            p.netRevenueEstimated() != null
                                ? p.netRevenueEstimated()
                                : BigDecimal.ZERO))
                .toList();
      }
      if (statsView.gameBreakdown() != null) {
        gameBreakdown =
            statsView.gameBreakdown().stream()
                .map(
                    g ->
                        new GameBreakdownItem(
                            g.gameCode() != null ? g.gameCode() : "",
                            g.gameLabel() != null ? g.gameLabel() : "",
                            g.ticketsSold(),
                            g.grossSales() != null ? g.grossSales() : BigDecimal.ZERO,
                            g.netRevenueEstimated() != null
                                ? g.netRevenueEstimated()
                                : BigDecimal.ZERO))
                .toList();
      }
    }

    return new TenantKpiSummaryPayload(
        total,
        active,
        suspended,
        onboarding,
        suspended,
        ticketsSoldToday,
        grossSalesToday,
        netToday,
        dailyBreakdown,
        gameBreakdown,
        topTenants);
  }

  private SubscriptionSummaryPayload buildSubscriptions() {
    PlatformSubscriptionStatsView view = queryBus.ask(new GetPlatformSubscriptionStatsQuery());
    if (view != null) {
      return new SubscriptionSummaryPayload(
          view.active(),
          0L,
          view.pastDue(),
          view.canceled(),
          view.total(),
          view.byPlan() != null ? view.byPlan() : List.of());
    }
    return SubscriptionSummaryPayload.empty();
  }

  private PlatformSalesPayload buildPlatformSales(TenantKpiSummaryPayload tenants) {
    return new PlatformSalesPayload(
        tenants.ticketsSoldToday(),
        tenants.grossSalesToday(),
        tenants.netToday(),
        tenants.dailyBreakdown(),
        tenants.gameBreakdown());
  }

  private TenantRankingPayload buildTenantRanking(TenantKpiSummaryPayload tenants) {
    return new TenantRankingPayload(tenants.topTenants());
  }

  /**
   * Tenants currently in DRAFT (onboarding in progress) — up to 10. Filtered in-memory; catalog
   * status filter is a V2 addition.
   */
  private OnboardingAlertsPayload buildOnboardingAlerts() {
    TchPage<TenantContextLookupView> page =
        tenantPreContextLookupApi.listTenants(PageRequest.of(0, 50));
    if (page != null && page.items() != null) {
      List<OnboardingAlertItem> items = new ArrayList<>();
      for (TenantContextLookupView t : page.items()) {
        if (t.status() == TenantStatus.DRAFT && items.size() < 10) {
          items.add(
              new OnboardingAlertItem(
                  t.tenantId().value().toString(),
                  t.code() != null ? t.code() : "",
                  t.displayName() != null ? t.displayName() : "",
                  t.status().name()));
        }
      }
      return new OnboardingAlertsPayload(List.copyOf(items), items.size());
    }
    return OnboardingAlertsPayload.empty();
  }

  private PublicContentPayload buildPublicContent() {
    List<PublicContentItemView> items =
        publicContentApi.listPlatformAdminDashboardNews(PUBLIC_CONTENT_LIMIT);
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
                            i.sourceType() != null ? i.sourceType().name() : "",
                            i.publishedAt() != null ? i.publishedAt().toString() : ""))
                .toList();
    return new PublicContentPayload(news, news.size());
  }

  private <T> T optionalSlice(
      com.tchalanet.server.common.web.error.ErrorDescriptor descriptor,
      String target,
      Supplier<T> supplier,
      Supplier<T> fallback) {
    return BffSlices.optional(
        BffSlicePolicy.warn(
                descriptor.code(),
                "features.platformadmin",
                NoticeSource.of("platformAdminDashboard").operation(target),
                fallback)
            .target(target),
        supplier::get);
  }

  private QuickActionsPayload buildQuickActions() {
    return new QuickActionsPayload(
        List.of(
            new ActionItem(
                "CREATE_TENANT",
                "quickaction.platform.create_tenant",
                "add_business",
                "/app/platform/tenants/new"),
            new ActionItem(
                "TENANT_ONBOARDING",
                "quickaction.platform.onboarding",
                "playlist_add_check",
                "/app/platform/tenants/onboarding"),
            new ActionItem(
                "SUBSCRIPTIONS",
                "quickaction.platform.subscriptions",
                "workspace_premium",
                "/app/platform/subscriptions"),
            new ActionItem(
                "PUBLIC_CONTENT",
                "quickaction.platform.public_content",
                "campaign",
                "/app/platform/news"),
            new ActionItem(
                "NOTIFICATIONS",
                "quickaction.platform.notifications",
                "notifications",
                "/app/platform/notifications")));
  }

  public record Payload(
      TenantKpiSummaryPayload tenants,
      PlatformSalesPayload sales,
      TenantRankingPayload tenantRanking,
      SubscriptionSummaryPayload subscriptions,
      OnboardingAlertsPayload onboardingAlerts,
      PublicContentPayload publicContent,
      QuickActionsPayload quickActions) {}

  // ---- surface-specific typed payload records ----

  /** Single tenant row in the "top tenants by volume" ranking. */
  public record TenantRankItem(
      String tenantCode, long ticketsSold, BigDecimal grossSales, BigDecimal netRevenue) {}

  /** Daily platform trend point for chart widgets. */
  public record DailyTrendItem(
      String refDate, long ticketsSold, BigDecimal grossSales, BigDecimal netRevenue) {}

  /** Game-level platform breakdown for chart/list widgets. */
  public record GameBreakdownItem(
      String gameCode,
      String gameLabel,
      long ticketsSold,
      BigDecimal grossSales,
      BigDecimal netRevenue) {}

  /** Tenant KPI summary — counts + analytics for today. */
  public record TenantKpiSummaryPayload(
      long total,
      long active,
      long suspended,
      long onboarding,
      long actionRequired,
      long ticketsSoldToday,
      BigDecimal grossSalesToday,
      BigDecimal netToday,
      List<DailyTrendItem> dailyBreakdown,
      List<GameBreakdownItem> gameBreakdown,
      List<TenantRankItem> topTenants) {}

  /** Platform sales analytics slice for KPI, chart and breakdown widgets. */
  public record PlatformSalesPayload(
      long ticketsSoldToday,
      BigDecimal grossSalesToday,
      BigDecimal netToday,
      List<DailyTrendItem> dailyBreakdown,
      List<GameBreakdownItem> gameBreakdown) {}

  /**
   * Dedicated top-tenants slice; avoids exposing the full platform KPI payload to ranking widgets.
   */
  public record TenantRankingPayload(List<TenantRankItem> topTenants) {}

  /** Subscription counts across all tenants. {@code byPlan} is the raw catalog projection. */
  public record SubscriptionSummaryPayload(
      long active, long trial, long pastDue, long expired, long total, List<?> byPlan) {

    public static SubscriptionSummaryPayload empty() {
      return new SubscriptionSummaryPayload(0L, 0L, 0L, 0L, 0L, List.of());
    }
  }

  /** Single tenant currently in DRAFT / onboarding state. */
  public record OnboardingAlertItem(String tenantId, String code, String name, String status) {}

  /** List of tenants in onboarding — up to 10. */
  public record OnboardingAlertsPayload(List<OnboardingAlertItem> items, int count) {

    public static OnboardingAlertsPayload empty() {
      return new OnboardingAlertsPayload(List.of(), 0);
    }
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

    void logPlatformCommercial(TchRequestContext ctx) {
      log.warn(
          "dashboard_timing surface=platform_admin_commercial totalMs={} userId={} blocks={}",
          elapsedMs(startedAt),
          ctx != null && ctx.userId() != null ? ctx.userId().value() : null,
          durationsMs);
    }

    private static long elapsedMs(long startedAt) {
      return (System.nanoTime() - startedAt) / 1_000_000L;
    }
  }
}
