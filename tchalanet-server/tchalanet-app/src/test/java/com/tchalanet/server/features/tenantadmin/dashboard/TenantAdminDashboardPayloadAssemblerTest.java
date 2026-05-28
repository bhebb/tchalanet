package com.tchalanet.server.features.tenantadmin.dashboard;

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
import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatus;
import com.tchalanet.server.catalog.tenant.api.model.TenantType;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.core.outlet.api.query.ListOutletsByTenantQuery;
import com.tchalanet.server.core.outlet.api.query.OutletView;
import com.tchalanet.server.core.seller.api.query.ListSellersQuery;
import com.tchalanet.server.core.seller.api.query.model.SellerSummaryView;
import com.tchalanet.server.core.terminal.api.query.ListTerminalsQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantAdminDashboardPayloadAssemblerTest {

  private final TenantCatalog tenantCatalog = mock(TenantCatalog.class);
  private final GameCatalog gameCatalog = mock(GameCatalog.class);
  private final DrawChannelCatalog drawChannelCatalog = mock(DrawChannelCatalog.class);
  private final QueryBus queryBus = mock(QueryBus.class);
  private final PublicContentApi publicContentApi = mock(PublicContentApi.class);

  private final TenantAdminDashboardPayloadAssembler assembler =
      new TenantAdminDashboardPayloadAssembler(
          tenantCatalog, gameCatalog, drawChannelCatalog, queryBus, publicContentApi);

  private final TenantId tenantId = TenantId.of(UUID.randomUUID());

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
    when(tenantCatalog.findRegistryById(tenantId)).thenReturn(Optional.of(registry()));
    when(queryBus.ask(any(ListOutletsByTenantQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListSellersQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListTerminalsQuery.class))).thenReturn(emptyPage());
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
    LocalDate today = LocalDate.now();
    var summary = new TenantDashboardStatsView.TenantSummaryCard(
        42L, new BigDecimal("123.45"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
    var dailyPoint = new TenantDashboardStatsView.TenantDailyPoint(
        today, 42L, new BigDecimal("123.45"));
    var statsView = new TenantDashboardStatsView(today.minusDays(1), today, summary,
        List.of(dailyPoint), List.of());

    when(queryBus.ask(any(GetTenantDashboardStatsQuery.class))).thenReturn(statsView);
    when(tenantCatalog.findRegistryById(tenantId)).thenReturn(Optional.empty());
    when(queryBus.ask(any(ListOutletsByTenantQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListSellersQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListTerminalsQuery.class))).thenReturn(emptyPage());
    when(gameCatalog.listActive()).thenReturn(List.of());
    when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var payload = assembler.assemble(context(tenantId));

    assertThat(payload.kpis().salesToday()).isEqualByComparingTo(new BigDecimal("123.45"));
    assertThat(payload.kpis().ticketCountToday()).isEqualTo(42L);
    assertThat(payload.kpis().activeSessions()).isEqualTo(0L);
    assertThat(payload.kpis().openDraws()).isEqualTo(0L);
    assertThat(payload.kpis().pendingApprovals()).isEqualTo(0L);
  }

  @Test
  @DisplayName("readiness is MISSING when every bundle is empty")
  void readinessMissing() {
    when(tenantCatalog.findRegistryById(tenantId)).thenReturn(Optional.empty());
    when(queryBus.ask(any(ListOutletsByTenantQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListSellersQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListTerminalsQuery.class))).thenReturn(emptyPage());
    when(gameCatalog.listActive()).thenReturn(List.of());
    when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var payload = assembler.assemble(context(tenantId));

    assertThat(payload.readiness().status()).isEqualTo("MISSING");
    assertThat(payload.readiness().missingCount()).isEqualTo(6);
  }

  @Test
  @DisplayName("operations + commercial counts derived from grouped bundles")
  void operationsAndCommercial() {
    when(tenantCatalog.findRegistryById(tenantId)).thenReturn(Optional.of(registry()));
    when(queryBus.ask(any(ListOutletsByTenantQuery.class)))
        .thenReturn(List.of(mock(OutletView.class), mock(OutletView.class)));
    when(queryBus.ask(any(ListSellersQuery.class)))
        .thenReturn(List.of(mock(SellerSummaryView.class)));
    when(queryBus.ask(any(ListTerminalsQuery.class))).thenReturn(pageWithTotal(3L));
    when(gameCatalog.listActive()).thenReturn(List.of(mock(GameView.class)));
    when(drawChannelCatalog.listAll(any(), any()))
        .thenReturn(List.of(mock(DrawChannelSummaryView.class), mock(DrawChannelSummaryView.class)));
    when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

    var payload = assembler.assemble(context(tenantId));

    assertThat(payload.operations().outlets().count()).isEqualTo(2);
    assertThat(payload.operations().terminals().count()).isEqualTo(3L);
    assertThat(payload.operations().users().count()).isEqualTo(1);
    assertThat(payload.commercial().gamesPricing().count()).isEqualTo(1);
    assertThat(payload.commercial().drawChannels().count()).isEqualTo(2);

    // Single bundle invocation — each grouped read called exactly once per assemble.
    verify(queryBus, times(1)).ask(any(ListOutletsByTenantQuery.class));
    verify(queryBus, times(1)).ask(any(ListSellersQuery.class));
    verify(queryBus, times(1)).ask(any(ListTerminalsQuery.class));
    verify(gameCatalog, times(1)).listActive();
    verify(drawChannelCatalog, times(1)).listAll(any(), any());
  }

  // ---------------------- helpers ----------------------

  private TchRequestContext context(TenantId boundTenant) {
    return new TchRequestContext(
        "tenant-demo",
        boundTenant != null ? boundTenant.value() : null,
        "tenant-demo",
        boundTenant != null ? boundTenant.value() : null,
        UUID.randomUUID().toString(),
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
        null);
  }

  private TenantRegistryView registry() {
    return new TenantRegistryView(
        tenantId, "tenant-demo", "Demo Tenant",
        TenantStatus.ACTIVE, TenantType.BORLETTE,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        Optional.empty(), Optional.empty());
  }

  @SuppressWarnings("unchecked")
  private TchPage<TerminalSummaryView> emptyPage() {
    return new TchPage<>(List.of(), 0, 1, 0L, 0, true, false, false);
  }

  @SuppressWarnings("unchecked")
  private TchPage<TerminalSummaryView> pageWithTotal(long total) {
    return new TchPage<>(List.of(), 0, 1, total, 1, true, false, false);
  }
}
