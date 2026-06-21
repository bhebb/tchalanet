package com.tchalanet.server.features.cashier.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.core.sales.api.query.CashierDashboardOverviewView;
import com.tchalanet.server.core.sales.api.query.GetCashierDashboardOverviewQuery;
import com.tchalanet.server.core.sales.api.query.ListCashierRecentTicketsQuery;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CashierDashboardPayloadAssemblerTest {

  private final QueryBus queryBus = mock(QueryBus.class);
  private final CashierDashboardPayloadAssembler assembler =
      new CashierDashboardPayloadAssembler(queryBus);

  private final TenantId tenantId = TenantId.of(UUID.randomUUID());
  private final UserId userId = UserId.of(UUID.randomUUID());
  private final SellerTerminalId sellerTerminalId = SellerTerminalId.of(UUID.randomUUID());

  @Nested
  @DisplayName("empty context")
  class EmptyContext {

    @Test
    @DisplayName("returns Payload.empty when ctx is null")
    void nullContext() {
      var payload = assembler.assemble(null);

      assertThat(payload.identity().cashierDisplayName()).isEmpty();
      assertThat(payload.session().active()).isFalse();
      assertThat(payload.readiness().ready()).isFalse();
      assertThat(payload.readiness().missing()).containsExactly("SELLER_TERMINAL");
      assertThat(payload.alerts().count()).isEqualTo(0);
      verify(queryBus, never()).ask(any());
    }

    @Test
    @DisplayName("returns Payload.empty when tenantId is null")
    void missingTenantId() {
      var ctx = context(TchActorType.SELLER_TERMINAL, sellerTerminalId, false, true);

      var payload = assembler.assemble(ctx);

      assertThat(payload.session().active()).isFalse();
      verify(queryBus, never()).ask(any());
    }
  }

  @Nested
  @DisplayName("grouped reads")
  class GroupedReads {

    @Test
    @DisplayName("V0 loads overview, nextDraws and recentTickets")
    void v0LoadsDashboardReads() {
      stubDashboardQueries();

      var payload = assembler.assemble(context(TchActorType.SELLER_TERMINAL, sellerTerminalId, true, true));

      verify(queryBus, times(1)).ask(any(GetCashierDashboardOverviewQuery.class));
      verify(queryBus, times(1)).ask(any(ListCashierNextDrawsQuery.class));
      verify(queryBus, times(1)).ask(any(ListCashierRecentTicketsQuery.class));
      assertThat(payload.identity().cashierDisplayName()).isEqualTo(sellerTerminalId.value().toString());
      assertThat(payload.session().active()).isTrue();
      assertThat(payload.overview().sessionOpen()).isTrue();
      assertThat(payload.overview().ticketCount()).isEqualTo(12L);
    }
  }

  @Nested
  @DisplayName("readiness")
  class Readiness {

    @Test
    @DisplayName("seller-terminal context is ready")
    void readyWhenSellerTerminalPresent() {
      stubDashboardQueries();

      var payload = assembler.assemble(context(TchActorType.SELLER_TERMINAL, sellerTerminalId, true, true));

      assertThat(payload.readiness().ready()).isTrue();
      assertThat(payload.readiness().trusted()).isTrue();
      assertThat(payload.readiness().source()).isEqualTo("SELLER_TERMINAL");
      assertThat(payload.readiness().missing()).isEmpty();
      assertThat(payload.alerts().items()).isEmpty();
    }

    @Test
    @DisplayName("missing seller-terminal reports blocker")
    void reportsMissingSellerTerminal() {
      stubDashboardQueries();

      var payload = assembler.assemble(context(TchActorType.APP_USER, null, true, true));

      assertThat(payload.readiness().ready()).isFalse();
      assertThat(payload.readiness().missing()).containsExactly("SELLER_TERMINAL");
      assertThat(payload.alerts().items())
          .singleElement()
          .satisfies(a -> assertThat(a.code()).isEqualTo("SELLER_TERMINAL_MISSING"));
    }
  }

  private void stubDashboardQueries() {
    when(queryBus.ask(any(GetCashierDashboardOverviewQuery.class))).thenReturn(sampleOverview());
    when(queryBus.ask(any(ListCashierNextDrawsQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListCashierRecentTicketsQuery.class))).thenReturn(List.of());
  }

  private static CashierDashboardOverviewView sampleOverview() {
    return new CashierDashboardOverviewView(
        java.time.LocalDate.parse("2026-05-21"), 12L, 50000L, 0L, 0L, List.of());
  }

  private TchRequestContext context(
      TchActorType actorType,
      SellerTerminalId terminalId,
      boolean hasTenantId,
      boolean hasUserId) {
    return new TchRequestContext(
        "tenant-demo",
        hasTenantId ? tenantId.value() : null,
        "tenant-demo",
        hasTenantId ? tenantId.value() : null,
        UUID.randomUUID().toString(),
        hasUserId ? userId.value() : null,
        Set.of(TchRole.CASHIER),
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
        hasTenantId ? tenantId : null,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null,
        actorType,
        terminalId,
        Set.of(),
        Set.of("ticket.sell"),
        null);
  }
}
