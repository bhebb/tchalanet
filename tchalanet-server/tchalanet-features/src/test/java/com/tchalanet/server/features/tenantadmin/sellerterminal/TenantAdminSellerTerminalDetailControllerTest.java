package com.tchalanet.server.features.tenantadmin.sellerterminal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import com.tchalanet.server.core.analytics.api.query.GetTenantFinancialBreakdownQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListAvailableLimitRulesQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticCategory;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantConfigView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantAdminSellerTerminalDetailControllerTest {

  private final QueryBus queryBus = mock(QueryBus.class);
  private final ClientDiagnosticsApi clientDiagnosticsApi = mock(ClientDiagnosticsApi.class);
  private final TenantConfigApi tenantConfigApi = mock(TenantConfigApi.class);
  private final TenantAdminSellerTerminalDetailController controller =
      new TenantAdminSellerTerminalDetailController(
          queryBus, clientDiagnosticsApi, tenantConfigApi);

  private final TenantId tenantId =
      TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
  private final SellerTerminalId sellerTerminalId =
      SellerTerminalId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));

  @Test
  void detail_returns_single_aggregate_for_admin_detail_page() {
    var terminal = sellerTerminal();
    var tenant = tenant();
    var diagnostics = diagnostics();
    var stats = todayStats();
    when(queryBus.ask(any(GetSellerTerminalQuery.class))).thenReturn(terminal);
    when(tenantConfigApi.getTenantById(any(GetTenantByIdRequest.class))).thenReturn(tenant);
    when(clientDiagnosticsApi.getPolicy(tenantId, sellerTerminalId)).thenReturn(diagnostics);
    when(queryBus.ask(any(ListAvailableLimitRulesQuery.class))).thenReturn(List.of());
    when(queryBus.ask(any(ListLimitAssignmentsByScopeQuery.class)))
        .thenAnswer(
            invocation ->
                new ListLimitAssignmentsView(LimitScopeQueryRef.tenant(tenantId), List.of()));
    when(queryBus.ask(any(GetTenantFinancialBreakdownQuery.class)))
        .thenReturn(
            new TenantFinancialBreakdownView(
                LocalDate.parse("2026-08-22"),
                LocalDate.parse("2026-08-22"),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(stats)));

    var response = controller.detail(context(), sellerTerminalId).data();

    assertThat(response.terminal()).isSameAs(terminal);
    assertThat(response.tenant().displayName()).isEqualTo("Tchalanet");
    assertThat(response.clientDiagnostics()).isSameAs(diagnostics);
    assertThat(response.todayStats()).isSameAs(stats);
    assertThat(response.limits().specs()).isEmpty();
    assertThat(response.limits().assignments()).isEmpty();
    assertThat(response.limits().inheritedAssignments()).isEmpty();
  }

  private SellerTerminalView sellerTerminal() {
    return new SellerTerminalView(
        sellerTerminalId,
        tenantId,
        "POS-001",
        "Terminal Test",
        "Ada",
        "Lovelace",
        "ada@tchalanet.test",
        "+50912345678",
        null,
        SellerTerminalStatus.ACTIVE,
        new BigDecimal("12.50"),
        Instant.parse("2026-08-22T11:00:00Z"),
        Instant.parse("2026-08-01T12:00:00Z"),
        null,
        null,
        null,
        null,
        false,
        null);
  }

  private TenantConfigView tenant() {
    return new TenantConfigView(
        tenantId,
        "tchalanet",
        "Tchalanet LLC",
        "Tchalanet",
        TenantType.BORLETTE,
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        TenantStatus.ACTIVE,
        null,
        null,
        null,
        null,
        new BigDecimal("12.50"),
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));
  }

  private ClientDiagnosticPolicyView diagnostics() {
    return new ClientDiagnosticPolicyView(
        true,
        Instant.parse("2026-08-22T13:00:00Z"),
        100,
        Set.of(ClientDiagnosticCategory.API, ClientDiagnosticCategory.CONNECTIVITY),
        "Support",
        Instant.parse("2026-08-22T11:00:00Z"));
  }

  private TenantFinancialBreakdownView.SellerTerminalDailyFinancialRow todayStats() {
    return new TenantFinancialBreakdownView.SellerTerminalDailyFinancialRow(
        sellerTerminalId.value(),
        LocalDate.parse("2026-08-22"),
        2,
        new BigDecimal("10.00"),
        new BigDecimal("1.30"),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0,
        0,
        new BigDecimal("8.70"),
        new BigDecimal("8.70"));
  }

  private TchRequestContext context() {
    return new TchRequestContext(
        "tchalanet",
        tenantId.value(),
        "tchalanet",
        tenantId.value(),
        UserId.of(UUID.randomUUID()).value(),
        Set.of(),
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
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null,
        TchActorType.APP_USER,
        null,
        Set.of(),
        Set.of(),
        null);
  }
}
