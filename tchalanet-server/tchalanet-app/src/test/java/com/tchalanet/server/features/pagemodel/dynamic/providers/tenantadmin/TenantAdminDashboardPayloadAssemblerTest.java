package com.tchalanet.server.features.pagemodel.dynamic.providers.tenantadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustStateView;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import com.tchalanet.server.core.analytics.api.query.GetAnalyticsTrustStateQuery;
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.core.analytics.api.query.GetTenantKpisQuery;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.ListDrawsQuery;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.features.pagemodel.contract.ActionItem;
import com.tchalanet.server.features.pagemodel.dashboard.DashboardPeriod;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantAdminDashboardPayloadAssemblerTest {

  private final TenantPreContextLookupApi tenantCatalog = mock(TenantPreContextLookupApi.class);
  private final GameCatalog gameCatalog = mock(GameCatalog.class);
  private final DrawChannelCatalog drawChannelCatalog = mock(DrawChannelCatalog.class);
  private final QueryBus queryBus = mock(QueryBus.class);
  private final PublicContentApi publicContentApi = mock(PublicContentApi.class);
  private final NotificationApi notificationApi = mock(NotificationApi.class);

  private final TenantAdminDashboardPayloadAssembler assembler =
      new TenantAdminDashboardPayloadAssembler(
          tenantCatalog,
          gameCatalog,
          drawChannelCatalog,
          queryBus,
          publicContentApi,
          notificationApi);

  private final TenantId tenantId = TenantId.of(UUID.randomUUID());

  @BeforeEach
  void analyticsTrustIsReadyByDefault() {
    when(queryBus.ask(any(GetAnalyticsTrustStateQuery.class)))
        .thenAnswer(
            invocation -> {
              var query = invocation.getArgument(0, GetAnalyticsTrustStateQuery.class);
              return AnalyticsTrustStateView.ready(query.scope(), Instant.now());
            });
  }

  @AfterEach
  void clearResponseContext() {
    ApiResponseContext.clear();
  }

  @Test
  @DisplayName("returns Payload.empty when ctx is null")
  void nullContext() {
    var payload = assembler.assemble(null);

    assertThat(payload.header().tenantName()).isEmpty();
    assertThat(payload.kpis().salesToday()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("returns Payload.empty when ctx has no tenant id")
  void noTenant() {
    var payload = assembler.assemble(context(null));

    assertThat(payload.header().tenantName()).isEmpty();
  }

  @Test
  @DisplayName("header includes tenant catalog data when available")
  void headerEnriched() {
    when(tenantCatalog.findById(tenantId)).thenReturn(Optional.of(registry()));
    when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(emptyPage());
    when(gameCatalog.listActive()).thenReturn(List.of());
    when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var payload = assembler.assemble(context(tenantId));

    assertThat(payload.header().tenantName()).isEqualTo("Demo Tenant");
    assertThat(payload.header().tenantStatus()).isEqualTo("ACTIVE");
    assertThat(payload.header().tenantType()).isEqualTo("BORLETTE");
  }

  @Test
  @DisplayName("KPIs propagate sales/tickets from analytics query")
  void kpisFromStats() {
    LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Port-au-Prince"));
    var summary =
        new TenantDashboardStatsView.TenantSummaryCard(
            42L,
            new BigDecimal("123.45"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0L,
            0L,
            BigDecimal.ZERO,
            0L);
    var dailyPoint =
        new TenantDashboardStatsView.TenantDailyPoint(
            today,
            42L,
            new BigDecimal("123.45"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0L,
            BigDecimal.ZERO);
    var gameBreakdown =
        new TenantDashboardStatsView.TenantGameBreakdown(
            "BORLETTE", "Borlette", 40L, new BigDecimal("120.00"), new BigDecimal("98.00"));
    var statsView =
        new TenantDashboardStatsView(
            today.minusDays(1), today, summary, List.of(dailyPoint), List.of(gameBreakdown));
    var kpisView =
        new TenantKpisView(
            42L,
            new BigDecimal("123.45"),
            new BigDecimal("15.00"),
            new BigDecimal("108.45"),
            0L,
            0L);

    when(queryBus.ask(any(GetTenantDashboardStatsQuery.class))).thenReturn(statsView);
    when(queryBus.ask(any(GetTenantKpisQuery.class))).thenReturn(kpisView);
    when(queryBus.ask(any(ListDrawsQuery.class))).thenReturn(drawPageWithTotal(3L));
    when(tenantCatalog.findById(tenantId)).thenReturn(Optional.empty());
    when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(emptyPage());
    when(gameCatalog.listActive()).thenReturn(List.of());
    when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var payload = assembler.assemble(context(tenantId));

    assertThat(payload.kpis().salesToday()).isEqualByComparingTo(new BigDecimal("123.45"));
    assertThat(payload.kpis().ticketCountToday()).isEqualTo(42L);
    assertThat(payload.kpis().activeSellerTerminals()).isEqualTo(0L);
    assertThat(payload.kpis().openDraws()).isEqualTo(3L);
    assertThat(payload.kpis().pendingApprovals()).isEqualTo(0L);
    assertThat(payload.salesTrend().points()).hasSize(7);
    assertThat(payload.salesTrend().points().get(6).grossSales())
        .isEqualByComparingTo(new BigDecimal("123.45"));
    assertThat(payload.gameBreakdown().items()).hasSize(1);
    assertThat(payload.gameBreakdown().items().get(0).gameCode()).isEqualTo("BORLETTE");
  }

  @Test
  @DisplayName("operations + commercial counts derived from grouped bundles")
  void operationsAndCommercial() {
    when(tenantCatalog.findById(tenantId)).thenReturn(Optional.of(registry()));
    when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(pageWithTotal(3L));
    when(gameCatalog.listActive()).thenReturn(List.of(mock(GameView.class)));
    when(drawChannelCatalog.listAll(any(), any()))
        .thenReturn(
            List.of(mock(DrawChannelSummaryView.class), mock(DrawChannelSummaryView.class)));
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var payload = assembler.assemble(context(tenantId));

    assertThat(payload.operations().outlets().status()).isEqualTo("PARKED");
    assertThat(payload.operations().outlets().count()).isZero();
    assertThat(payload.operations().terminals().count()).isEqualTo(3L);
    assertThat(payload.operations().users().count()).isZero();
    assertThat(payload.commercial().gamesPricing().count()).isEqualTo(1);
    assertThat(payload.commercial().drawChannels().count()).isEqualTo(2);

    // Single bundle invocation — each grouped read called exactly once per assemble.
    verify(queryBus, times(2)).ask(any(ListSellerTerminalsQuery.class));
    verify(gameCatalog, times(1)).listActive();
    verify(drawChannelCatalog, times(1)).listAll(any(), any());
  }

  @Test
  @DisplayName("operational KPIs use the selected tenant-local period and comparison window")
  void operationalKpisUseSelectedPeriod() {
    var today = LocalDate.now(java.time.ZoneId.of("America/Port-au-Prince"));
    var weekStart = today.with(java.time.DayOfWeek.MONDAY);
    when(queryBus.ask(any(GetTenantDashboardStatsQuery.class)))
        .thenAnswer(
            invocation -> {
              var query = invocation.getArgument(0, GetTenantDashboardStatsQuery.class);
              boolean current = query.from().equals(weekStart);
              return stats(
                  query.from(),
                  query.to(),
                  current ? new BigDecimal("100.00") : new BigDecimal("80.00"),
                  current ? new BigDecimal("10.00") : new BigDecimal("8.00"),
                  current ? new BigDecimal("60.00") : new BigDecimal("50.00"));
            });
    when(tenantCatalog.findById(tenantId)).thenReturn(Optional.empty());
    when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(emptyPage());
    when(gameCatalog.listActive()).thenReturn(List.of());
    when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var payload = assembler.assemble(context(tenantId), DashboardPeriod.THIS_WEEK, 0);

    assertThat(payload.operationalKpis().period()).isEqualTo("THIS_WEEK");
    assertThat(payload.operationalKpis().comparisonPeriod()).isEqualTo("PREVIOUS_WEEK");
    assertThat(payload.operationalKpis().fromDate()).isNotBlank();
    assertThat(payload.operationalKpis().toDate()).isNotBlank();
    assertThat(payload.operationalKpis().grossSales().value())
        .isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(payload.operationalKpis().grossSales().delta())
        .isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(payload.operationalKpis().grossSales().deltaPercent())
        .isEqualByComparingTo(new BigDecimal("25.0000"));
  }

  @Test
  @DisplayName("sales trend uses a seven-day window ending at the selected period")
  void salesTrendUsesSelectedPeriodUpperBound() {
    var queries = new ArrayList<GetTenantDashboardStatsQuery>();
    when(queryBus.ask(any(GetTenantDashboardStatsQuery.class)))
        .thenAnswer(
            invocation -> {
              var query = invocation.getArgument(0, GetTenantDashboardStatsQuery.class);
              queries.add(query);
              return stats(
                  query.from(), query.to(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            });
    when(tenantCatalog.findById(tenantId)).thenReturn(Optional.empty());
    when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(emptyPage());
    when(gameCatalog.listActive()).thenReturn(List.of());
    when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var selectedEnd = LocalDate.now(java.time.ZoneId.of("America/Port-au-Prince")).minusDays(1);
    assembler.assemble(context(tenantId), DashboardPeriod.YESTERDAY, 0);

    assertThat(queries)
        .anySatisfy(
            query -> {
              assertThat(query.from()).isEqualTo(selectedEnd.minusDays(6));
              assertThat(query.to()).isEqualTo(selectedEnd);
            });
  }

  @Test
  @DisplayName("quick actions match the tenant admin dashboard model")
  void quickActionsMatchDashboardModel() {
    when(tenantCatalog.findById(tenantId)).thenReturn(Optional.empty());
    when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(emptyPage());
    when(gameCatalog.listActive()).thenReturn(List.of());
    when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var actions = assembler.assemble(context(tenantId)).quickActions().actions();

    assertThat(actions)
        .extracting(ActionItem::id)
        .containsExactly(
            "TODAYS_DRAWS",
            "DAILY_REPORT",
            "SELLER_TERMINALS",
            "CREATE_SELLER_TERMINAL",
            "SALES_BY_TERMINAL",
            "SALES_BY_DRAW",
            "BLOCK_NUMBER",
            "BLOCK_SELLER",
            "RESET_SELLER_PIN",
            "DRAW_RESULTS");
    assertThat(actions)
        .extracting(action -> action.destination().value())
        .containsExactly(
            "/app/admin/draws?date=TODAY",
            "/app/admin/reports/daily",
            "/app/admin/seller-terminals",
            "/app/admin/seller-terminals/new",
            "/app/admin/reports/sellers",
            "/app/admin/reports/draws",
            "/app/admin/limits/number",
            "/app/admin/seller-terminals",
            "/app/admin/seller-terminals",
            "/app/admin/draws/results");
    assertThat(actions)
        .extracting(ActionItem::labelKey)
        .containsSubsequence(
            "quickaction.admin.seller_terminals", "quickaction.admin.seller_terminal_add");
  }

  @Test
  @DisplayName("analytics failure is an unavailable section, never a zero-sales result")
  void analyticsFailureIsExplicit() {
    when(queryBus.ask(any(GetAnalyticsTrustStateQuery.class)))
        .thenAnswer(
            invocation -> {
              var query = invocation.getArgument(0, GetAnalyticsTrustStateQuery.class);
              return AnalyticsTrustStateView.unavailable(
                  query.scope(), List.of(LocalDate.now()), Instant.now());
            });
    when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(emptyPage());
    when(gameCatalog.listActive()).thenReturn(List.of());
    when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var payload = assembler.assemble(context(tenantId));

    assertThat(payload.sectionStates().items())
        .anySatisfy(
            section -> {
              assertThat(section.id()).isEqualTo("analytics");
              assertThat(section.status()).isEqualTo("UNAVAILABLE");
            });
    assertThat(payload.sectionStates().items())
        .anySatisfy(
            section -> {
              assertThat(section.id()).isEqualTo("kpis");
              assertThat(section.status()).isEqualTo("UNAVAILABLE");
            });
    assertThat(ApiResponseContext.get().getNotices())
        .anySatisfy(
            notice -> {
              assertThat(notice.code()).isEqualTo("tenantadmin.dashboard.analytics_unavailable");
              assertThat(notice.target()).isEqualTo("tenant_admin_dashboard.analytics");
            });
  }

  // ---------------------- helpers ----------------------

  private TchRequestContext context(TenantId boundTenant) {
    return new TchRequestContext(
        "tenant-demo",
        boundTenant != null ? boundTenant.value() : null,
        "tenant-demo",
        boundTenant != null ? boundTenant.value() : null,
        UUID.randomUUID(),
        Set.of(TchRole.TENANT_ADMIN),
        Set.of(),
        Locale.FRANCE,
        "req-test",
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        boundTenant,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private TenantContextLookupView registry() {
    return new TenantContextLookupView(
        tenantId,
        "tenant-demo",
        "Demo Tenant",
        "Demo Tenant",
        TenantStatus.ACTIVE,
        TenantType.BORLETTE,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        "fr",
        "fr-HT",
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  @SuppressWarnings("unchecked")
  private TchPage<SellerTerminalSummaryRow> emptyPage() {
    return new TchPage<>(List.of(), 0, 1, 0L, 0, true, false, false);
  }

  @SuppressWarnings("unchecked")
  private TchPage<SellerTerminalSummaryRow> pageWithTotal(long total) {
    return new TchPage<>(List.of(), 0, 1, total, 1, true, false, false);
  }

  private TchPage<DrawSummary> drawPageWithTotal(long total) {
    return new TchPage<>(List.of(), 0, 1, total, 1, true, false, false);
  }

  private TenantDashboardStatsView stats(
      LocalDate from, LocalDate to, BigDecimal grossSales, BigDecimal commission, BigDecimal net) {
    return new TenantDashboardStatsView(
        from,
        to,
        new TenantDashboardStatsView.TenantSummaryCard(
            1L,
            grossSales,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            commission,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0L,
            0L,
            net,
            0L),
        List.of(),
        List.of());
  }
}
