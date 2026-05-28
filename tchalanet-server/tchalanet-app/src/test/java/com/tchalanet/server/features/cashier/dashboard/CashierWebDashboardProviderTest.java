package com.tchalanet.server.features.cashier.dashboard;

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
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CashierWebDashboardProviderTest {

  private final CashierDashboardPayloadAssembler assembler =
      mock(CashierDashboardPayloadAssembler.class);
  private final CashierWebDashboardProvider provider =
      new CashierWebDashboardProvider(assembler);

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

      assertThat(provider.load(null, "dashboard.cashier.next_draws", null, "fr", null,
          new PageModelResolutionContext()))
          .isInstanceOfSatisfying(Map.class, m -> assertThat(m.get("items")).isEqualTo(payload.nextDraws()));
      assertThat(provider.load(null, "dashboard.cashier.recent_tickets", null, "fr", null,
          new PageModelResolutionContext()))
          .isInstanceOfSatisfying(Map.class, m -> assertThat(m.get("items")).isEqualTo(payload.recentTickets()));
    }

    @Test
    @DisplayName("quick_sale returns a static descriptor")
    void quickSale() {
      when(assembler.assemble(any())).thenReturn(samplePayload());

      Object result = provider.load(null, "dashboard.cashier.quick_sale", null, "fr", null,
          new PageModelResolutionContext());

      assertThat(result).isInstanceOfSatisfying(Map.class, m -> {
        assertThat(m.get("actionId")).isEqualTo("SELL_TICKET");
        assertThat(m.get("route")).isEqualTo("/cashier/sell");
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
      provider.load(null, "dashboard.cashier.next_draws", null, "fr", null, ctx);
      provider.load(null, "dashboard.cashier.recent_tickets", null, "fr", null, ctx);

      verify(assembler, times(1)).assemble(any());
    }
  }

  private static CashierDashboardPayloadAssembler.Payload samplePayload() {
    return new CashierDashboardPayloadAssembler.Payload(
        Map.of("cashierDisplayName", "Caissier Test"),
        Map.of("active", true, "sessionRef", "S-001"),
        Map.of("sessionOpen", true, "ticketCount", 5L),
        List.of(Map.of("drawId", "d-1")),
        List.of(Map.of("ticketId", "t-1")),
        Map.of("ready", true, "trusted", true, "source", "SIGNED_DEVICE_BINDING", "missing", List.of()),
        Map.of("count", 0, "items", List.of()),
        Map.of("stats", 0, "items", List.of()),
        Map.of("offlineSync", 0, "items", List.of())
        );
  }
}
