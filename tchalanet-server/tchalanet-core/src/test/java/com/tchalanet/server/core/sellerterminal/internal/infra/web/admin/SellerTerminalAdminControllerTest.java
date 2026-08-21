package com.tchalanet.server.core.sellerterminal.internal.infra.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.analytics.api.model.TenantKpisView;
import com.tchalanet.server.core.analytics.api.query.GetTenantKpisQuery;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticBatchRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventDetailView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionContext;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionResult;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerTerminalAdminControllerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));

  @Test
  void summary_uses_tenant_analytics_for_today() {
    var controller =
        new SellerTerminalAdminController(
            new NoopCommandBus(), new SummaryQueryBus(), new NoopClientDiagnosticsApi());

    var response = controller.summary(context());

    assertThat(response.data().activeCount()).isEqualTo(1);
    assertThat(response.data().blockedCount()).isEqualTo(1);
    assertThat(response.data().salesTodayAmount()).isEqualByComparingTo("15.00");
    assertThat(response.data().currency()).isEqualTo("HTG");
  }

  private static TchRequestContext context() {
    return new TchRequestContext(
        "demo",
        TENANT_ID.value(),
        "demo",
        TENANT_ID.value(),
        UUID.fromString("10000000-0000-0000-0000-000000000002"),
        Set.of(),
        Set.of(),
        Locale.FRENCH,
        "req-1",
        "127.0.0.1",
        "test",
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        TENANT_ID,
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null,
        null,
        null,
        Set.of(),
        Set.of(),
        null);
  }

  private static final class SummaryQueryBus implements QueryBus {
    @Override
    @SuppressWarnings("unchecked")
    public <R> R ask(Query<R> query) {
      if (query instanceof ListSellerTerminalsQuery) {
        var active =
            new SellerTerminalSummaryRow(
                SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001")),
                TENANT_ID,
                "POS-001",
                "Main",
                null,
                null,
                SellerTerminalStatus.ACTIVE,
                new BigDecimal("0.13"),
                null,
                null,
                null,
                null,
                null,
                null);
        var blocked =
            new SellerTerminalSummaryRow(
                SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000002")),
                TENANT_ID,
                "POS-002",
                "Backup",
                null,
                null,
                SellerTerminalStatus.BLOCKED,
                new BigDecimal("0.10"),
                null,
                null,
                null,
                null,
                null,
                null);
        return (R) TchPage.of(List.of(active, blocked), 0, 500, 2, 1, true, false, false);
      }
      if (query instanceof GetTenantKpisQuery) {
        return (R)
            new TenantKpisView(
                1L, new BigDecimal("15.00"), BigDecimal.ZERO, BigDecimal.ZERO, 0L, 1L);
      }
      throw new AssertionError("Unexpected query: " + query.getClass().getSimpleName());
    }
  }

  private static final class NoopCommandBus implements CommandBus {
    @Override
    public <R> R execute(Command<R> command) {
      throw new AssertionError("No command expected");
    }
  }

  private static final class NoopClientDiagnosticsApi implements ClientDiagnosticsApi {
    @Override
    public ClientDiagnosticIngestionResult ingest(
        ClientDiagnosticIngestionContext context, ClientDiagnosticBatchRequest request) {
      throw new AssertionError("No diagnostics ingestion expected");
    }

    @Override
    public ClientDiagnosticPolicyView getPolicy(
        TenantId tenantId, SellerTerminalId sellerTerminalId) {
      return ClientDiagnosticPolicyView.disabled();
    }

    @Override
    public ClientDiagnosticPolicyView enablePolicy(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        ClientDiagnosticPolicyRequest request,
        UUID actorUserId) {
      throw new AssertionError("No diagnostics enable expected");
    }

    @Override
    public ClientDiagnosticPolicyView disablePolicy(
        TenantId tenantId, SellerTerminalId sellerTerminalId, UUID actorUserId) {
      throw new AssertionError("No diagnostics disable expected");
    }

    @Override
    public List<ClientDiagnosticEventView> recentEvents(
        TenantId tenantId, SellerTerminalId sellerTerminalId, int limit) {
      throw new AssertionError("No diagnostics events expected");
    }

    @Override
    public ClientDiagnosticEventDetailView eventDetail(UUID eventId) {
      throw new AssertionError("No diagnostics detail expected");
    }
  }
}
