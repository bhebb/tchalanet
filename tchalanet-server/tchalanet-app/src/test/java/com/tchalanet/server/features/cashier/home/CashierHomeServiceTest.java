package com.tchalanet.server.features.cashier.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.query.CashierRecentTicketView;
import com.tchalanet.server.core.sales.api.query.ListCashierRecentTicketsQuery;
import com.tchalanet.server.core.session.api.query.CashierSessionSummaryView;
import com.tchalanet.server.core.session.api.query.GetCashierSessionSummaryQuery;
import com.tchalanet.server.features.cashier.draws.CashierAvailableDrawView;
import com.tchalanet.server.features.cashier.draws.CashierDrawsService;
import com.tchalanet.server.features.cashier.home.app.CashierHomeService;
import com.tchalanet.server.features.cashier.home.app.ClientSurfaceResolver;
import com.tchalanet.server.features.cashier.home.model.HomeRequiredStepType;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CashierHomeServiceTest {

  private final QueryBus queryBus = mock(QueryBus.class);
  private final CashierDrawsService drawsService = mock(CashierDrawsService.class);
  private final CashierHomeService service =
      new CashierHomeService(new ClientSurfaceResolver(), drawsService, queryBus);

  private final TenantId tenantId = TenantId.of(UUID.randomUUID());
  private final UserId userId = UserId.of(UUID.randomUUID());

  @BeforeEach
  void setUp() {
    when(queryBus.ask(any())).thenAnswer(invocation -> {
      Object query = invocation.getArgument(0);
      if (query instanceof GetCashierSessionSummaryQuery) {
        return openSession();
      }
      if (query instanceof ListCashierRecentTicketsQuery) {
        return List.of(new CashierRecentTicketView(
            "PUB-1", "ACCEPTED", Instant.parse("2026-05-21T12:05:00Z"), 12500, 0, "Texas", 2));
      }
      return null;
    });
    when(drawsService.listAvailable(any(), anyInt(), anyInt()))
        .thenReturn(List.of(primaryDraw()));
  }

  @Test
  void cashier_home_mobile_ready_returns_sell_action() {
    var response = service.mobileHome(context(Set.of(TchRole.CASHIER), true), "MOBILE_POS");

    assertThat(response.surface()).isEqualTo(ClientSurface.MOBILE_POS);
    assertThat(response.requiredStep()).isNull();
    assertThat(response.primaryAction().type()).isEqualTo("SELL_TICKET");
    assertThat(response.session().open()).isTrue();
    assertThat(response.primaryDraw()).isNotNull();
  }

  @Test
  void cashier_home_mobile_missing_context_returns_select_context_required_step() {
    var response = service.mobileHome(context(Set.of(TchRole.CASHIER), false), null);

    assertThat(response.requiredStep().type()).isEqualTo(HomeRequiredStepType.SELECT_OPERATIONAL_CONTEXT);
    assertThat(response.primaryAction().route()).isEqualTo("/operational-context/select");
  }

  @Test
  void cashier_home_mobile_closed_session_returns_open_session_required_step() {
    when(queryBus.ask(any())).thenAnswer(invocation -> {
      Object query = invocation.getArgument(0);
      if (query instanceof GetCashierSessionSummaryQuery) {
        return CashierSessionSummaryView.inactive();
      }
      return List.of();
    });

    var response = service.mobileHome(context(Set.of(TchRole.CASHIER), true), "MOBILE_POS");

    assertThat(response.requiredStep().type()).isEqualTo(HomeRequiredStepType.OPEN_SESSION);
    assertThat(response.primaryAction().route()).isEqualTo("/session/open");
  }

  @Test
  void cashier_home_mobile_does_not_return_long_dashboard() {
    var response = service.mobileHome(context(Set.of(TchRole.CASHIER), true), "MOBILE_POS");

    assertThat(response.widgets()).extracting("key")
        .containsExactly("session_status", "primary_draw");
    assertThat(response.widgets()).extracting("key")
        .doesNotContain("pending_approvals", "limits", "top_selections");
  }

  // Note: cashier web home is now served by the PageModel cashier dashboard
  // (source cashier_dashboard) — see CashierWebDashboardProvider. The legacy
  // service.webHome() endpoint was removed in dashboard-overview-runtime-v1 Vague 2.

  @Test
  void surface_header_not_allowed_returns_403() {
    assertThatThrownBy(() -> service.mobileHome(context(Set.of(TchRole.CASHIER), true), "PLATFORM_ADMIN_WEB"))
        .isInstanceOf(ProblemRestException.class)
        .hasMessageContaining("surface.not_allowed");
  }

  @Test
  void missing_surface_header_uses_preferred_surface() {
    var response = service.mobileHome(context(Set.of(TchRole.CASHIER), true), null);

    assertThat(response.surface()).isEqualTo(ClientSurface.MOBILE_POS);
  }

  private CashierSessionSummaryView openSession() {
    return new CashierSessionSummaryView(
        true,
        "session-ref",
        Instant.parse("2026-05-21T12:00:00Z"),
        0,
        238500,
        47);
  }

  private CashierAvailableDrawView primaryDraw() {
    return new CashierAvailableDrawView(
        DrawId.of(UUID.randomUUID()),
        DrawChannelId.of(UUID.randomUUID()),
        LocalDate.of(2026, 5, 21),
        null,
        "10:00",
        "TEXAS",
        "Haiti - Texas - 10:00",
        List.of("BOLET"),
        "OPEN",
        Instant.parse("2026-05-21T15:00:00Z"),
        Instant.now().plusSeconds(1_800));
  }

  private TchRequestContext context(Set<TchRole> roles, boolean trustedOperationalContext) {
    var operationalContext = trustedOperationalContext
        ? new OperationalContextHint(
            TerminalId.of(UUID.randomUUID()),
            OutletId.of(UUID.randomUUID()),
            SalesSessionId.of(UUID.randomUUID()),
            OperationalContextSource.SIGNED_DEVICE_BINDING,
            OperationalContextTrust.STRONG)
        : null;
    return new TchRequestContext(
        "tenant-demo",
        tenantId.value(),
        "tenant-demo",
        tenantId.value(),
        UUID.randomUUID().toString(),
        userId.value(),
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
        tenantId,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        operationalContext);
  }
}
