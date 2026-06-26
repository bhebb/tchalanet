package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.subscription.api.query.GetPlatformSubscriptionStatsQuery;
import com.tchalanet.server.core.subscription.api.query.PlatformSubscriptionStatsView;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantStatsView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformAdminDashboardPayloadAssemblerTest {

    private final TenantPreContextLookupApi tenantCatalog = mock(TenantPreContextLookupApi.class);
    private final QueryBus queryBus = mock(QueryBus.class);
    private final PublicContentApi publicContentApi = mock(PublicContentApi.class);

    private final PlatformAdminDashboardPayloadAssembler assembler =
        new PlatformAdminDashboardPayloadAssembler(tenantCatalog, queryBus, publicContentApi);

    @Test
    @DisplayName("tenants KPI come from TenantCatalog.stats")
    void tenantsKpi() {
        when(tenantCatalog.stats()).thenReturn(new TenantStatsView(15, 12, 2));
        when(tenantCatalog.listTenants(any())).thenReturn(emptyPage());
        when(publicContentApi.listPlatformAdminDashboardNews(any(int.class))).thenReturn(List.of());

        var payload = assembler.assemble(null);

        assertThat(payload.tenants().total()).isEqualTo(15L);
        assertThat(payload.tenants().active()).isEqualTo(12L);
        assertThat(payload.tenants().suspended()).isEqualTo(2L);
    }

    @Test
    @DisplayName("subscriptions come from GetPlatformSubscriptionStatsQuery")
    void subscriptionsFromQueryBus() {
        when(tenantCatalog.stats()).thenReturn(new TenantStatsView(0, 0, 0));
        when(tenantCatalog.listTenants(any())).thenReturn(emptyPage());
        when(publicContentApi.listPlatformAdminDashboardNews(any(int.class))).thenReturn(List.of());
        when(queryBus.ask(any(GetPlatformSubscriptionStatsQuery.class)))
            .thenReturn(new PlatformSubscriptionStatsView(10, 7, 1, 2, List.of()));

        var payload = assembler.assemble(null);

        assertThat(payload.subscriptions().active()).isEqualTo(7L);
        assertThat(payload.subscriptions().pastDue()).isEqualTo(1L);
        assertThat(payload.subscriptions().expired()).isEqualTo(2L);
        assertThat(payload.subscriptions().total()).isEqualTo(10L);
    }

    @Test
    @DisplayName("subscriptions fall back to zero when query throws")
    void subscriptionsFallback() {
        when(tenantCatalog.stats()).thenReturn(new TenantStatsView(0, 0, 0));
        when(tenantCatalog.listTenants(any())).thenReturn(emptyPage());
        when(publicContentApi.listPlatformAdminDashboardNews(any(int.class))).thenReturn(List.of());
        when(queryBus.ask(any(GetPlatformSubscriptionStatsQuery.class)))
            .thenThrow(new RuntimeException("boom"));

        var payload = assembler.assemble(null);

        assertThat(payload.subscriptions().active()).isEqualTo(0L);
        assertThat(payload.subscriptions().total()).isEqualTo(0L);
    }

    private static <T> TchPage<T> emptyPage() {
        return new TchPage<>(List.of(), 0, 50, 0L, 0, true, false, false);
    }
}
