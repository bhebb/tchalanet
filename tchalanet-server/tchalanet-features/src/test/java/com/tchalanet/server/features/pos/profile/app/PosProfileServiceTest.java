package com.tchalanet.server.features.pos.profile.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalCommercialCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalContactCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalLabelCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalSettingsQuery;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileCommercialRequest;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileSellerRequest;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileTerminalRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PosProfileServiceTest {

  private final CommandBus commandBus = mock(CommandBus.class);
  private final QueryBus queryBus = mock(QueryBus.class);
  private final ClientDiagnosticsApi clientDiagnosticsApi = mock(ClientDiagnosticsApi.class);
  private final PosProfileService service =
      new PosProfileService(commandBus, queryBus, clientDiagnosticsApi);

  private final TenantId tenantId = TenantId.of(UUID.randomUUID());
  private final UserId userId = UserId.of(UUID.randomUUID());
  private final SellerTerminalId sellerTerminalId = SellerTerminalId.of(UUID.randomUUID());

  @BeforeEach
  void setUp() {
    when(queryBus.ask(any(GetSellerTerminalQuery.class))).thenReturn(sellerTerminal());
    when(queryBus.ask(any(GetSellerTerminalSettingsQuery.class)))
        .thenReturn(SellerTerminalSettingsView.defaults());
    when(clientDiagnosticsApi.getPolicy(any(TenantId.class), any(SellerTerminalId.class)))
        .thenReturn(ClientDiagnosticPolicyView.disabled());
  }

  @Test
  void profile_returns_single_pos_account_payload() {
    var response = service.profile(context());

    assertThat(response.version()).isEqualTo("profile.v1");
    assertThat(response.terminal().id()).isEqualTo(sellerTerminalId.value().toString());
    assertThat(response.terminal().displayName()).isEqualTo("Terminal Central");
    assertThat(response.terminal().ready()).isTrue();
    assertThat(response.seller().displayName()).isEqualTo("Ada Lovelace");
    assertThat(response.seller().email()).isEqualTo("ada@tchalanet.test");
    assertThat(response.commercial().tenantCode()).isEqualTo("tenant-demo");
    assertThat(response.commercial().currency()).isEqualTo("HTG");
    assertThat(response.commercial().commissionRate()).isEqualByComparingTo("12.50");
    assertThat(response.settings().locale()).isEqualTo("fr-FR");
    assertThat(response.settings().timezone()).isEqualTo("America/Port-au-Prince");
    assertThat(response.settings().supportedLocales()).containsExactly("ht", "fr", "en");
    assertThat(response.settings().receipt().autoPrint()).isTrue();
    assertThat(response.settings().receipt().copyCount()).isEqualTo(1);
    assertThat(response.settings().notifications().enabled()).isTrue();
  }

  @Test
  void update_terminal_dispatches_label_command_and_returns_refreshed_profile() {
    var response =
        service.updateTerminal(context(), new UpdatePosProfileTerminalRequest("Terminal Nord"));

    var captor = ArgumentCaptor.forClass(UpdateSellerTerminalLabelCommand.class);
    verify(commandBus).execute(captor.capture());
    assertThat(captor.getValue().displayName()).isEqualTo("Terminal Nord");
    assertThat(captor.getValue().sellerTerminalId()).isEqualTo(sellerTerminalId);
    assertThat(response.version()).isEqualTo("profile.v1");
  }

  @Test
  void update_seller_dispatches_contact_command_without_touching_terminal_label() {
    service.updateSeller(
        context(),
        new UpdatePosProfileSellerRequest("Grace", "Hopper", "grace@test.me", "+509", null));

    var captor = ArgumentCaptor.forClass(UpdateSellerTerminalContactCommand.class);
    verify(commandBus).execute(captor.capture());
    assertThat(captor.getValue().firstName()).isEqualTo("Grace");
    assertThat(captor.getValue().lastName()).isEqualTo("Hopper");
    assertThat(captor.getValue().email()).isEqualTo("grace@test.me");
    assertThat(captor.getValue().phoneNumber()).isEqualTo("+509");
  }

  @Test
  void update_commercial_dispatches_commercial_command() {
    service.updateCommercial(
        context(), new UpdatePosProfileCommercialRequest(new BigDecimal("14.25")));

    var captor = ArgumentCaptor.forClass(UpdateSellerTerminalCommercialCommand.class);
    verify(commandBus).execute(captor.capture());
    assertThat(captor.getValue().commissionRate()).isEqualByComparingTo("14.25");
  }

  private SellerTerminalView sellerTerminal() {
    return new SellerTerminalView(
        sellerTerminalId,
        tenantId,
        "POS-001",
        "Terminal Central",
        "Ada",
        "Lovelace",
        "ada@tchalanet.test",
        "+50912345678",
        null,
        SellerTerminalStatus.ACTIVE,
        new BigDecimal("12.50"),
        Instant.parse("2026-07-21T20:00:00Z"),
        Instant.parse("2026-07-01T12:00:00Z"),
        null,
        null,
        null,
        null,
        false,
        null);
  }

  private TchRequestContext context() {
    return new TchRequestContext(
        "tenant-demo",
        tenantId.value(),
        "tenant-demo",
        tenantId.value(),
        userId.value(),
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
        TchActorType.SELLER_TERMINAL,
        sellerTerminalId,
        Set.of("ACTOR_SELLER_TERMINAL"),
        Set.of("ticket.sell"),
        null);
  }
}
