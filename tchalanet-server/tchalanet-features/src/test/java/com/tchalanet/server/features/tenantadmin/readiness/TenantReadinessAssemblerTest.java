package com.tchalanet.server.features.tenantadmin.readiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSection;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessStatus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import com.tchalanet.server.features.tenantadmin.setup.TenantDrawSalesMatrixService;
import com.tchalanet.server.features.tenantadmin.setup.TenantGamesPricingService;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenanttheme.api.TenantThemeApi;
import java.util.List;
import org.junit.jupiter.api.Test;

class TenantReadinessAssemblerTest {

  private final TenantReadinessAssembler assembler =
      new TenantReadinessAssembler(
          mock(TenantPreContextLookupApi.class),
          mock(AddressApi.class),
          mock(TenantConfigApi.class),
          mock(QueryBus.class),
          mock(TenantGamesPricingService.class),
          mock(TenantDrawSalesMatrixService.class),
          mock(TenantThemeApi.class));

  @Test
  void generated_draw_is_a_blocking_setup_step() {
    var setup = assembler.computeSetup(viewWith(TenantReadinessStatus.MISSING, TenantReadinessStatus.READY));

    assertThat(setup.totalSteps()).isEqualTo(5);
    assertThat(setup.completedSteps()).isEqualTo(4);
    assertThat(setup.status()).isEqualTo("INCOMPLETE");
    assertThat(setup.canCreateSellerTerminal()).isFalse();
    assertThat(setup.blockingSteps()).containsExactly("GENERATED_DRAWS");
  }

  @Test
  void unknown_theme_does_not_block_an_operationally_ready_tenant() {
    var setup = assembler.computeSetup(viewWith(TenantReadinessStatus.READY, TenantReadinessStatus.UNKNOWN));

    assertThat(setup.completedSteps()).isEqualTo(5);
    assertThat(setup.status()).isEqualTo("COMPLETE");
    assertThat(setup.canCreateSellerTerminal()).isTrue();
    assertThat(setup.blockingSteps()).isEmpty();
  }

  private TenantReadinessView viewWith(
      TenantReadinessStatus generatedDrawsStatus, TenantReadinessStatus themeStatus) {
    return new TenantReadinessView(
        TenantReadinessStatus.READY,
        0,
        List.of(
            section("identity", TenantReadinessStatus.READY),
            section("address", TenantReadinessStatus.READY),
            section("settings", TenantReadinessStatus.READY),
            section("games_pricing", TenantReadinessStatus.READY),
            section("draws", TenantReadinessStatus.READY),
            section("generated_draws", generatedDrawsStatus),
            section("theme", themeStatus)));
  }

  private TenantReadinessSection section(String id, TenantReadinessStatus status) {
    return new TenantReadinessSection(id, id, status, "/app/admin/" + id, List.of());
  }
}
