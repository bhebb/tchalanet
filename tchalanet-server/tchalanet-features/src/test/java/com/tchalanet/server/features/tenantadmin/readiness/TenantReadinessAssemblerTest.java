package com.tchalanet.server.features.tenantadmin.readiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessIssue;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSection;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessStatus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import com.tchalanet.server.features.tenantadmin.setup.TenantDrawSalesMatrixService;
import com.tchalanet.server.features.tenantadmin.setup.TenantGamesPricingService;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView.ReadinessStatus;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView.SectionReadiness;
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
    var setup =
        assembler.computeSetup(
            viewWith(TenantReadinessStatus.MISSING, TenantReadinessStatus.READY));

    assertThat(setup.totalSteps()).isEqualTo(5);
    assertThat(setup.completedSteps()).isEqualTo(4);
    assertThat(setup.status()).isEqualTo("INCOMPLETE");
    assertThat(setup.canCreateSellerTerminal()).isFalse();
    assertThat(setup.blockingSteps()).containsExactly("GENERATED_DRAWS");
  }

  @Test
  void unknown_theme_does_not_block_an_operationally_ready_tenant() {
    var setup =
        assembler.computeSetup(
            viewWith(TenantReadinessStatus.READY, TenantReadinessStatus.UNKNOWN));

    assertThat(setup.completedSteps()).isEqualTo(5);
    assertThat(setup.status()).isEqualTo("COMPLETE");
    assertThat(setup.canCreateSellerTerminal()).isTrue();
    assertThat(setup.blockingSteps()).isEmpty();
  }

  @Test
  void print_settings_reasons_are_operational_and_do_not_block_setup() {
    var settingsReadiness =
        new TenantSettingsReadinessView(
            false,
            List.of(
                new SectionReadiness(
                    "print",
                    ReadinessStatus.ACTION_REQUIRED,
                    List.of("settings.print.paper_size_missing"),
                    List.of())));

    assertThat(TenantReadinessAssembler.settingsSectionStatus(settingsReadiness))
        .isEqualTo(TenantReadinessStatus.READY);
    assertThat(TenantReadinessAssembler.settingsBlockingReasonCodes(settingsReadiness))
        .containsExactly("settings.print.paper_size_missing");

    var setup =
        assembler.computeSetup(
            viewWithSettings(
                TenantReadinessStatus.READY,
                List.of(
                    new TenantReadinessIssue(
                        "settings", "settings.print.paper_size_missing", "/app/admin/settings"))));

    assertThat(setup.completedSteps()).isEqualTo(5);
    assertThat(setup.status()).isEqualTo("COMPLETE");
    assertThat(setup.canCreateSellerTerminal()).isTrue();
    assertThat(setup.blockingSteps()).isEmpty();
  }

  @Test
  void non_print_settings_reasons_still_block_setup_progress() {
    var settingsReadiness =
        new TenantSettingsReadinessView(
            false,
            List.of(
                new SectionReadiness(
                    "defaults",
                    ReadinessStatus.ACTION_REQUIRED,
                    List.of("settings.defaults.currency_missing"),
                    List.of())));

    assertThat(TenantReadinessAssembler.settingsSectionStatus(settingsReadiness))
        .isEqualTo(TenantReadinessStatus.MISSING);
    assertThat(
            TenantReadinessAssembler.isOperationalSettingsReason(
                "settings.defaults.currency_missing"))
        .isFalse();

    var setup =
        assembler.computeSetup(
            viewWithSettings(
                TenantReadinessStatus.MISSING,
                List.of(
                    new TenantReadinessIssue(
                        "settings", "settings.defaults.currency_missing", "/app/admin/settings"))));

    assertThat(setup.completedSteps()).isEqualTo(4);
    assertThat(setup.status()).isEqualTo("INCOMPLETE");
    assertThat(setup.nextRecommendedStep()).isEqualTo("SETTINGS");
  }

  private TenantReadinessView viewWith(
      TenantReadinessStatus generatedDrawsStatus, TenantReadinessStatus themeStatus) {
    return viewWithSettings(
        TenantReadinessStatus.READY, List.of(), generatedDrawsStatus, themeStatus);
  }

  private TenantReadinessView viewWithSettings(
      TenantReadinessStatus settingsStatus, List<TenantReadinessIssue> settingsIssues) {
    return viewWithSettings(
        settingsStatus, settingsIssues, TenantReadinessStatus.READY, TenantReadinessStatus.UNKNOWN);
  }

  private TenantReadinessView viewWithSettings(
      TenantReadinessStatus settingsStatus,
      List<TenantReadinessIssue> settingsIssues,
      TenantReadinessStatus generatedDrawsStatus,
      TenantReadinessStatus themeStatus) {
    return new TenantReadinessView(
        TenantReadinessStatus.READY,
        0,
        List.of(
            section("identity", TenantReadinessStatus.READY),
            section("address", TenantReadinessStatus.READY),
            section("settings", settingsStatus, settingsIssues),
            section("games_pricing", TenantReadinessStatus.READY),
            section("draws", TenantReadinessStatus.READY),
            section("generated_draws", generatedDrawsStatus),
            section("theme", themeStatus)));
  }

  private TenantReadinessSection section(String id, TenantReadinessStatus status) {
    return section(id, status, List.of());
  }

  private TenantReadinessSection section(
      String id, TenantReadinessStatus status, List<TenantReadinessIssue> issues) {
    return new TenantReadinessSection(id, id, status, "/app/admin/" + id, issues);
  }
}
