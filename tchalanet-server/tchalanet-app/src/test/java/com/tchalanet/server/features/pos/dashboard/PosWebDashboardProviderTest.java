package com.tchalanet.server.features.pos.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PosWebDashboardProviderTest {

  private final PosDashboardPayloadAssembler assembler =
      mock(PosDashboardPayloadAssembler.class);
  private final PosWebDashboardProvider provider =
      new PosWebDashboardProvider(assembler);

  @Nested
  @DisplayName("supports")
  class Supports {

    @Test
    @DisplayName("matches source cashier_dashboard only")
    void matchesSource() {
      assertThat(provider.supports(
          "private.dashboard.cashier.web", "CashierIdentityWidget", "cashier_dashboard"))
          .isTrue();
      assertThat(provider.supports(
          "private.dashboard.cashier.web", "CashierIdentityWidget", "tenant_admin_dashboard"))
          .isFalse();
    }
  }

  @Nested
  @DisplayName("dispatch by widgetId")
  class Dispatch {

    @Test
    @DisplayName("identity / session / overview / next_draws / recent_tickets / readiness / alerts route to expected payload slice")
    void allKnownWidgets() {
      var payload = samplePayload();
      when(assembler.assemble(any())).thenReturn(payload);

      assertThat(provider.load(null, "dashboard.cashier.identity", null, "fr", null,
          new PageModelResolutionContext()))
          .isEqualTo(payload.identity());
      assertThat(provider.load(null, "dashboard.cashier.session", null, "fr", null,
          new PageModelResolutionContext()))
          .isEqualTo(payload.session());
      assertThat(provider.load(null, "dashboard.cashier.overview", null, "fr", null,
          new PageModelResolutionContext()))
          .isEqualTo(payload.overview());
      assertThat(provider.load(null, "dashboard.cashier.readiness", null, "fr", null,
          new PageModelResolutionContext()))
          .isEqualTo(payload.readiness());
      assertThat(provider.load(null, "dashboard.cashier.alerts", null, "fr", null,
          new PageModelResolutionContext()))
          .isEqualTo(payload.alerts());

      var nextDrawsResult = (PosWebDashboardProvider.ItemsPayload) provider.load(
          null, "dashboard.cashier.nextDraws", null, "fr", null, new PageModelResolutionContext());
      assertThat(nextDrawsResult.items()).isEqualTo(payload.nextDraws());
      var recentTicketsResult = (PosWebDashboardProvider.ItemsPayload) provider.load(
          null, "dashboard.cashier.recentTickets", null, "fr", null, new PageModelResolutionContext());
      assertThat(recentTicketsResult.items()).isEqualTo(payload.recentTickets());
    }

    @Test
    @DisplayName("quick_sale returns a static descriptor")
    void quickSale() {
      when(assembler.assemble(any())).thenReturn(samplePayload());

      Object result = provider.load(null, "dashboard.cashier.quickSale", null, "fr", null,
          new PageModelResolutionContext());

      assertThat(result).isInstanceOfSatisfying(PosWebDashboardProvider.QuickSalePayload.class, p -> {
        assertThat(p.actionId()).isEqualTo("sellTicket");
        assertThat(p.path()).isEqualTo("/cashier/sell");
      });
    }

    @Test
    @DisplayName("unknown widgetId raises CASHIER_DASHBOARD_UNKNOWN_WIDGET")
    void unknownWidget() {
      when(assembler.assemble(any())).thenReturn(samplePayload());

      assertThatThrownBy(() -> provider.load(
          null, "dashboard.cashier.unknown", null, "fr", null, new PageModelResolutionContext()))
          .isInstanceOfSatisfying(PageModelDynamicProviderException.class,
              e -> assertThat(e.code()).isEqualTo("CASHIER_DASHBOARD_UNKNOWN_WIDGET"));
    }
  }

  @Nested
  @DisplayName("memoization via PageModelResolutionContext")
  class Memo {

    @Test
    @DisplayName("multiple widgets in the same request share a single assembly")
    void sharedAssembly() {
      when(assembler.assemble(any())).thenReturn(samplePayload());

      var ctx = new PageModelResolutionContext();
      provider.load(null, "dashboard.cashier.identity", null, "fr", null, ctx);
      provider.load(null, "dashboard.cashier.session", null, "fr", null, ctx);
      provider.load(null, "dashboard.cashier.overview", null, "fr", null, ctx);
      provider.load(null, "dashboard.cashier.readiness", null, "fr", null, ctx);
      provider.load(null, "dashboard.cashier.alerts", null, "fr", null, ctx);
      provider.load(null, "dashboard.cashier.nextDraws", null, "fr", null, ctx);
      provider.load(null, "dashboard.cashier.recentTickets", null, "fr", null, ctx);

      verify(assembler, times(1)).assemble(any());
    }
  }

  private static PosDashboardPayloadAssembler.Payload samplePayload() {
    return new PosDashboardPayloadAssembler.Payload(
        new PosDashboardPayloadAssembler.CashierIdentityPayload(
            "Caissier Test", "Outlet Test", "T-001", "tenant-demo"),
        new PosDashboardPayloadAssembler.CashierSessionPayload(
            true, "S-001", "", 0L, 0L, 0L),
        PosDashboardPayloadAssembler.CashierOverviewPayload.noSession(),
        List.of(),
        List.of(),
        new PosDashboardPayloadAssembler.CashierReadinessPayload(
            true, true, "SIGNED_DEVICE_BINDING", List.of()),
        new PosDashboardPayloadAssembler.CashierAlertsPayload(0, List.of()),
        PosDashboardPayloadAssembler.CashierStatsPayload.unavailable(),
        new PosDashboardPayloadAssembler.CashierOfflineSyncPayload("UNKNOWN", 0));
  }
}
