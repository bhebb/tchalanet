package com.tchalanet.server.features.cashier.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.core.sales.api.query.CashierDashboardOverviewView;
import com.tchalanet.server.core.sales.api.query.GetCashierDashboardOverviewQuery;
import com.tchalanet.server.core.sales.api.query.ListCashierRecentTicketsQuery;
import com.tchalanet.server.core.session.api.query.CashierIdentityView;
import com.tchalanet.server.core.session.api.query.CashierSessionSummaryView;
import com.tchalanet.server.core.session.api.query.GetCashierIdentityQuery;
import com.tchalanet.server.core.session.api.query.GetCashierSessionSummaryQuery;
import java.time.Instant;
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
      assertThat(payload.alerts().count()).isEqualTo(0);
      verify(queryBus, never()).ask(any());
    }

    @Test
    @DisplayName("returns Payload.empty when tenantId is null")
    void missingTenantId() {
      var ctx = context(Set.of(TchRole.CASHIER), true, /* tenantId */ false, /* userId */ true);

      var payload = assembler.assemble(ctx);

      assertThat(payload.session().active()).isFalse();
      verify(queryBus, never()).ask(any());
    }
  }

  @Nested
  @DisplayName("grouped reads")
  class GroupedReads {

    @Test
    @DisplayName("session inactive → 4 grouped reads (identity, session, nextDraws, recentTickets) — overview skipped")
    void sessionInactiveSkipsOverview() {
      when(queryBus.ask(any(GetCashierIdentityQuery.class))).thenReturn(emptyIdentity());
      when(queryBus.ask(any(GetCashierSessionSummaryQuery.class)))
          .thenReturn(CashierSessionSummaryView.inactive());
      when(queryBus.ask(any(ListCashierNextDrawsQuery.class))).thenReturn(List.of());
      when(queryBus.ask(any(ListCashierRecentTicketsQuery.class))).thenReturn(List.of());

      assembler.assemble(context(Set.of(TchRole.CASHIER), true, true, true));

      verify(queryBus, times(1)).ask(any(GetCashierIdentityQuery.class));
      verify(queryBus, times(1)).ask(any(GetCashierSessionSummaryQuery.class));
      verify(queryBus, never()).ask(any(GetCashierDashboardOverviewQuery.class));
      verify(queryBus, times(1)).ask(any(ListCashierNextDrawsQuery.class));
      verify(queryBus, times(1)).ask(any(ListCashierRecentTicketsQuery.class));
    }

    @Test
    @DisplayName("session active → 5 grouped reads (overview included)")
    void sessionActiveLoadsOverview() {
      when(queryBus.ask(any(GetCashierIdentityQuery.class))).thenReturn(emptyIdentity());
      when(queryBus.ask(any(GetCashierSessionSummaryQuery.class)))
          .thenReturn(activeSession());
      when(queryBus.ask(any(GetCashierDashboardOverviewQuery.class)))
          .thenReturn(sampleOverview());
      when(queryBus.ask(any(ListCashierNextDrawsQuery.class))).thenReturn(List.of());
      when(queryBus.ask(any(ListCashierRecentTicketsQuery.class))).thenReturn(List.of());

      var payload = assembler.assemble(context(Set.of(TchRole.CASHIER), true, true, true));

      verify(queryBus, times(1)).ask(any(GetCashierDashboardOverviewQuery.class));
      assertThat(payload.session().active()).isTrue();
      assertThat(payload.overview().sessionOpen()).isTrue();
      assertThat(payload.overview().ticketCount()).isEqualTo(12L);
    }
  }

  @Nested
  @DisplayName("readiness (operational context)")
  class Readiness {

    @Test
    @DisplayName("trusted context with outlet + terminal → ready=true")
    void readyWhenAllPresent() {
      stubInactiveSessionQueries();

      var payload = assembler.assemble(context(Set.of(TchRole.CASHIER), true, true, true));

      assertThat(payload.readiness().ready()).isTrue();
      assertThat(payload.readiness().trusted()).isTrue();
      assertThat(payload.readiness().source()).isEqualTo(OperationalContextSource.SIGNED_DEVICE_BINDING.name());
      assertThat(payload.readiness().missing()).isEmpty();
    }

    @Test
    @DisplayName("untrusted context → ready=false even when outlet+terminal present")
    void untrustedNotReady() {
      stubInactiveSessionQueries();

      var ctx = contextWithHint(new OperationalContextHint(
          TerminalId.of(UUID.randomUUID()),
          OutletId.of(UUID.randomUUID()),
          null,
          OperationalContextSource.CLIENT_CLAIM,
          OperationalContextTrust.WEAK));

      var payload = assembler.assemble(ctx);

      assertThat(payload.readiness().ready()).isFalse();
      assertThat(payload.readiness().trusted()).isFalse();
    }

    @Test
    @DisplayName("missing outlet → reports OUTLET in missing list")
    void reportsMissingOutlet() {
      stubInactiveSessionQueries();

      var ctx = contextWithHint(new OperationalContextHint(
          TerminalId.of(UUID.randomUUID()),
          null,
          null,
          OperationalContextSource.SIGNED_DEVICE_BINDING,
          OperationalContextTrust.STRONG));

      var payload = assembler.assemble(ctx);

      assertThat(payload.readiness().missing()).contains("OUTLET");
    }
  }

  @Nested
  @DisplayName("alerts")
  class Alerts {

    @Test
    @DisplayName("emits BLOCKER for missing outlet and terminal")
    void blockersForMissingPos() {
      stubInactiveSessionQueries();

      var payload = assembler.assemble(contextWithHint(null));

      assertThat(payload.alerts().items())
          .anySatisfy(a -> assertThat(a.code()).isEqualTo("OUTLET_MISSING"))
          .anySatisfy(a -> assertThat(a.code()).isEqualTo("TERMINAL_MISSING"));
    }

    @Test
    @DisplayName("emits SESSION_CLOSED warning when session inactive")
    void sessionClosedWarning() {
      stubInactiveSessionQueries();

      var payload = assembler.assemble(context(Set.of(TchRole.CASHIER), true, true, true));

      assertThat(payload.alerts().items())
          .anySatisfy(a -> assertThat(a.code()).isEqualTo("SESSION_CLOSED"));
    }
  }

  // --- helpers --------------------------------------------------------------

  private void stubInactiveSessionQueries() {
    when(queryBus.ask(any(GetCashierIdentityQuery.class))).thenReturn(emptyIdentity());
    when(queryBus.ask(any(GetCashierSessionSummaryQuery.class)))
        .thenReturn(CashierSessionSummaryView.inactive());
    when(queryBus.ask(any(ListCashierNextDrawsQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListCashierRecentTicketsQuery.class))).thenReturn(List.of());
  }

  private static CashierIdentityView emptyIdentity() {
    return new CashierIdentityView("Caissier Test", "Outlet Test", "T-001", "tenant-demo");
  }

  private static CashierSessionSummaryView activeSession() {
    return new CashierSessionSummaryView(
        true, "S-001", Instant.parse("2026-05-21T08:00:00Z"), 10000L, 50000L, 12);
  }

  private static CashierDashboardOverviewView sampleOverview() {
    return new CashierDashboardOverviewView(
        java.time.LocalDate.parse("2026-05-21"), 12L, 50000L, 0L, 0L, List.of());
  }

  private TchRequestContext context(
      Set<TchRole> roles, boolean trustedHint,
      boolean hasTenantId, boolean hasUserId) {
    OperationalContextHint hint = trustedHint
        ? new OperationalContextHint(
            TerminalId.of(UUID.randomUUID()),
            OutletId.of(UUID.randomUUID()),
            SalesSessionId.of(UUID.randomUUID()),
            OperationalContextSource.SIGNED_DEVICE_BINDING,
            OperationalContextTrust.STRONG)
        : null;
    return new TchRequestContext(
        "tenant-demo",
        hasTenantId ? tenantId.value() : null,
        "tenant-demo",
        hasTenantId ? tenantId.value() : null,
        UUID.randomUUID().toString(),
        hasUserId ? userId.value() : null,
        roles,
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
        hint,
        null, null, null, null, null);
  }

  private TchRequestContext contextWithHint(OperationalContextHint hint) {
    return new TchRequestContext(
        "tenant-demo",
        tenantId.value(),
        "tenant-demo",
        tenantId.value(),
        UUID.randomUUID().toString(),
        userId.value(),
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
        tenantId,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        hint,
        null, null, null, null, null);
  }
}
