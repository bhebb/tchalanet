package com.tchalanet.server.features.platformadmin.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatsView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.subscription.api.query.GetPlatformSubscriptionStatsQuery;
import com.tchalanet.server.core.subscription.api.query.PlatformSubscriptionStatsView;
import com.tchalanet.server.features.stats.platformdashboard.PlatformDashboardStatsService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PlatformAdminDashboardPayloadAssemblerTest {

  private final PlatformDashboardStatsService statsService = mock(PlatformDashboardStatsService.class);
  private final TenantCatalog tenantCatalog = mock(TenantCatalog.class);
  private final QueryBus queryBus = mock(QueryBus.class);
  @SuppressWarnings("unchecked")
  private final ObjectProvider<PlatformHealthProbe> healthProvider = mock(ObjectProvider.class);

  private final PlatformAdminDashboardPayloadAssembler assembler =
      new PlatformAdminDashboardPayloadAssembler(statsService, tenantCatalog, queryBus, healthProvider);

  @Test
  @DisplayName("health is UNKNOWN when no probe is registered")
  void healthUnknownWithoutProbe() {
    when(healthProvider.getIfAvailable()).thenReturn(null);
    when(tenantCatalog.stats()).thenReturn(new TenantStatsView(0, 0, 0));
    when(tenantCatalog.listTenants(any())).thenReturn(emptyPage());

    var payload = assembler.assemble(null);

    assertThat(payload.health()).containsEntry("global", "UNKNOWN");
  }

  @Test
  @DisplayName("health is read once from the probe when available")
  void healthFromProbe() {
    PlatformHealthProbe probe = () -> Map.of("global", "UP", "components", Map.of("db", "UP"));
    when(healthProvider.getIfAvailable()).thenReturn(probe);
    when(tenantCatalog.stats()).thenReturn(new TenantStatsView(0, 0, 0));
    when(tenantCatalog.listTenants(any())).thenReturn(emptyPage());

    var payload = assembler.assemble(null);

    assertThat(payload.health()).containsEntry("global", "UP");
  }

  @Test
  @DisplayName("tenants KPI come from TenantCatalog.stats")
  void tenantsKpi() {
    when(healthProvider.getIfAvailable()).thenReturn(null);
    when(tenantCatalog.stats()).thenReturn(new TenantStatsView(15, 12, 2));
    when(tenantCatalog.listTenants(any())).thenReturn(emptyPage());

    var payload = assembler.assemble(null);

    assertThat(payload.tenants())
        .containsEntry("total", 15L)
        .containsEntry("active", 12L)
        .containsEntry("suspended", 2L);
  }

  @Test
  @DisplayName("subscriptions come from GetPlatformSubscriptionStatsQuery")
  void subscriptionsFromQueryBus() {
    when(healthProvider.getIfAvailable()).thenReturn(null);
    when(tenantCatalog.stats()).thenReturn(new TenantStatsView(0, 0, 0));
    when(tenantCatalog.listTenants(any())).thenReturn(emptyPage());
    when(queryBus.ask(any(GetPlatformSubscriptionStatsQuery.class)))
        .thenReturn(new PlatformSubscriptionStatsView(10, 7, 1, 2, List.of()));

    var payload = assembler.assemble(null);

    assertThat(payload.subscriptions())
        .containsEntry("active", 7L)
        .containsEntry("pastDue", 1L)
        .containsEntry("expired", 2L)
        .containsEntry("total", 10L);
  }

  @Test
  @DisplayName("subscriptions fall back to zero when query throws")
  void subscriptionsFallback() {
    when(healthProvider.getIfAvailable()).thenReturn(null);
    when(tenantCatalog.stats()).thenReturn(new TenantStatsView(0, 0, 0));
    when(tenantCatalog.listTenants(any())).thenReturn(emptyPage());
    when(queryBus.ask(any(GetPlatformSubscriptionStatsQuery.class)))
        .thenThrow(new RuntimeException("boom"));

    var payload = assembler.assemble(null);

    assertThat(payload.subscriptions())
        .containsEntry("active", 0L)
        .containsEntry("total", 0L);
  }

  private static <T> TchPage<T> emptyPage() {
    return new TchPage<>(List.of(), 0, 50, 0L, 0, true, false, false);
  }
}
