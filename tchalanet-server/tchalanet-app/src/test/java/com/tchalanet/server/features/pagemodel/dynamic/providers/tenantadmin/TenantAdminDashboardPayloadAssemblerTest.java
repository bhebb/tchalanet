package com.tchalanet.server.features.pagemodel.dynamic.providers.tenantadmin;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;
import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import com.tchalanet.server.core.analytics.api.query.GetTenantDashboardStatsQuery;
import com.tchalanet.server.core.analytics.api.query.GetTenantKpisQuery;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantAdminDashboardPayloadAssemblerTest {

    private final TenantPreContextLookupApi tenantCatalog = mock(TenantPreContextLookupApi.class);
    private final GameCatalog gameCatalog = mock(GameCatalog.class);
    private final DrawChannelCatalog drawChannelCatalog = mock(DrawChannelCatalog.class);
    private final QueryBus queryBus = mock(QueryBus.class);
    private final PublicContentApi publicContentApi = mock(PublicContentApi.class);
    private final NotificationApi notificationApi = mock(NotificationApi.class);

    private final TenantAdminDashboardPayloadAssembler assembler =
        new TenantAdminDashboardPayloadAssembler(
            tenantCatalog, gameCatalog, drawChannelCatalog, queryBus, publicContentApi, notificationApi);

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
        LocalDate today = LocalDate.now();
        var summary = new TenantDashboardStatsView.TenantSummaryCard(
            42L, new BigDecimal("123.45"), BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, 0L);
        var dailyPoint = new TenantDashboardStatsView.TenantDailyPoint(
            today, 42L, new BigDecimal("123.45"), BigDecimal.ZERO, BigDecimal.ZERO,
            0L, BigDecimal.ZERO, new BigDecimal("100.00"));
        var gameBreakdown = new TenantDashboardStatsView.TenantGameBreakdown(
            "BORLETTE", "Borlette", 40L, new BigDecimal("120.00"), new BigDecimal("98.00"));
        var statsView = new TenantDashboardStatsView(today.minusDays(1), today, summary,
            List.of(dailyPoint), List.of(gameBreakdown));
        var kpisView = new TenantKpisView(
            42L, new BigDecimal("123.45"), new BigDecimal("15.00"), new BigDecimal("108.45"), 0L, 0L);

        when(queryBus.ask(any(GetTenantDashboardStatsQuery.class))).thenReturn(statsView);
        when(queryBus.ask(any(GetTenantKpisQuery.class))).thenReturn(kpisView);
        when(tenantCatalog.findById(tenantId)).thenReturn(Optional.empty());
        when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(emptyPage());
        when(gameCatalog.listActive()).thenReturn(List.of());
        when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
        when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

        var payload = assembler.assemble(context(tenantId));

        assertThat(payload.kpis().salesToday()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(payload.kpis().ticketCountToday()).isEqualTo(42L);
        assertThat(payload.kpis().activeSellerTerminals()).isEqualTo(0L);
        assertThat(payload.kpis().openDraws()).isEqualTo(0L);
        assertThat(payload.kpis().pendingApprovals()).isEqualTo(0L);
        assertThat(payload.salesTrend().points()).hasSize(1);
        assertThat(payload.salesTrend().points().get(0).grossSales()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(payload.gameBreakdown().items()).hasSize(1);
        assertThat(payload.gameBreakdown().items().get(0).gameCode()).isEqualTo("BORLETTE");
    }

    @Test
    @DisplayName("readiness is MISSING when every bundle is empty")
    void readinessMissing() {
        when(tenantCatalog.findById(tenantId)).thenReturn(Optional.empty());
        when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(emptyPage());
        when(gameCatalog.listActive()).thenReturn(List.of());
        when(drawChannelCatalog.listAll(any(), any())).thenReturn(List.of());
        when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

        var payload = assembler.assemble(context(tenantId));

        assertThat(payload.readiness().status()).isEqualTo("MISSING");
        assertThat(payload.readiness().missingCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("operations + commercial counts derived from grouped bundles")
    void operationsAndCommercial() {
        when(tenantCatalog.findById(tenantId)).thenReturn(Optional.of(registry()));
        when(queryBus.ask(any(ListSellerTerminalsQuery.class))).thenReturn(pageWithTotal(3L));
        when(gameCatalog.listActive()).thenReturn(List.of(mock(GameView.class)));
        when(drawChannelCatalog.listAll(any(), any()))
            .thenReturn(List.of(mock(DrawChannelSummaryView.class), mock(DrawChannelSummaryView.class)));
        when(publicContentApi.listTenantAdminDashboardNews(any(int.class))).thenReturn(List.of());

        var payload = assembler.assemble(context(tenantId));

        assertThat(payload.operations().outlets().status()).isEqualTo("PARKED");
        assertThat(payload.operations().outlets().count()).isZero();
        assertThat(payload.operations().terminals().count()).isEqualTo(3L);
        assertThat(payload.operations().users().count()).isEqualTo(1);
        assertThat(payload.commercial().gamesPricing().count()).isEqualTo(1);
        assertThat(payload.commercial().drawChannels().count()).isEqualTo(2);

        // Single bundle invocation — each grouped read called exactly once per assemble.
        verify(queryBus, times(1)).ask(any(ListSellerTerminalsQuery.class));
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
            null,
            null, null, null, null, null);
    }

    private TenantContextLookupView registry() {
        return new TenantContextLookupView(
            tenantId, "tenant-demo", "Demo Tenant",
            TenantStatus.ACTIVE, TenantType.BORLETTE,
            java.time.ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            "fr", "fr-HT",
            Optional.empty(), Optional.empty(), Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private TchPage<SellerTerminalSummaryRow> emptyPage() {
        return new TchPage<>(List.of(), 0, 1, 0L, 0, true, false, false);
    }

    @SuppressWarnings("unchecked")
    private TchPage<SellerTerminalSummaryRow> pageWithTotal(long total) {
        return new TchPage<>(List.of(), 0, 1, total, 1, true, false, false);
    }
}
