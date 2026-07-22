package com.tchalanet.server.features.pos.profile.app;

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
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosProfileServiceTest {

  private final QueryBus queryBus = mock(QueryBus.class);
  private final PosProfileService service = new PosProfileService(queryBus);

  private final TenantId tenantId = TenantId.of(UUID.randomUUID());
  private final UserId userId = UserId.of(UUID.randomUUID());
  private final SellerTerminalId sellerTerminalId = SellerTerminalId.of(UUID.randomUUID());

  @Test
  void profile_returns_single_pos_account_payload() {
    when(queryBus.ask(any(GetSellerTerminalQuery.class))).thenReturn(sellerTerminal());

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
