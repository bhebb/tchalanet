package com.tchalanet.server.features.pos.home;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.features.pos.draws.PosAvailableDrawView;
import com.tchalanet.server.features.pos.draws.PosDrawsService;
import com.tchalanet.server.features.pos.home.app.ClientSurfaceResolver;
import com.tchalanet.server.features.pos.home.app.PosHomeService;
import com.tchalanet.server.features.pos.home.model.PosAttentionLevel;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PosHomeServiceTest {

    private final QueryBus queryBus = mock(QueryBus.class);
    private final PosDrawsService drawsService = mock(PosDrawsService.class);
    private final PosHomeService service =
        new PosHomeService(new ClientSurfaceResolver(), drawsService, queryBus);

    private final TenantId tenantId = TenantId.of(UUID.randomUUID());
    private final UserId userId = UserId.of(UUID.randomUUID());
    private final SellerTerminalId sellerTerminalId = SellerTerminalId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        when(queryBus.ask(any(GetSellerTerminalQuery.class))).thenReturn(sellerTerminal(SellerTerminalStatus.ACTIVE));
        when(drawsService.listAvailable(any(), anyInt(), anyInt()))
            .thenReturn(List.of(primaryDraw()));
    }

    @Test
    void seller_terminal_mobile_home_returns_sell_action() {
        var response = service.mobileHome(context(TchActorType.SELLER_TERMINAL, sellerTerminalId), "MOBILE_POS");

        assertThat(response.surface()).isEqualTo(ClientSurface.MOBILE_POS);
        assertThat(response.requiredStep()).isNull();
        assertThat(response.primaryAction().type()).isEqualTo("SELL_TICKET");
        assertThat(response.primaryAction().enabled()).isTrue();
        assertThat(response.operationalContext().ready()).isTrue();
        assertThat(response.operationalContext().source()).isEqualTo("SELLER_TERMINAL");
        assertThat(response.primaryDraw()).isNotNull();
    }

    @Test
    void non_seller_terminal_actor_is_rejected_for_mobile_home() {
        assertThatThrownBy(() -> service.mobileHome(context(TchActorType.APP_USER, null), "MOBILE_POS"))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("seller_terminal.actor_required");
    }

    @Test
    void surface_header_not_allowed_returns_403() {
        assertThatThrownBy(() -> service.mobileHome(
            context(TchActorType.SELLER_TERMINAL, sellerTerminalId), "PLATFORM_ADMIN_WEB"))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("surface.not_allowed");
    }

    @Test
    void missing_surface_header_uses_preferred_surface() {
        var response = service.mobileHome(context(TchActorType.SELLER_TERMINAL, sellerTerminalId), null);

        assertThat(response.surface()).isEqualTo(ClientSurface.MOBILE_POS);
    }

    @Test
    void readiness_requires_seller_terminal_actor() {
        var response = service.readiness(context(TchActorType.APP_USER, null));

        assertThat(response.ready()).isFalse();
        assertThat(response.attentionLevel()).isEqualTo(PosAttentionLevel.BLOCKED);
        assertThat(response.blockers().get(0).params()).containsEntry("missing", List.of("SELLER_TERMINAL"));
    }

    @Test
    void readiness_for_seller_terminal_is_ready() {
        var response = service.readiness(context(TchActorType.SELLER_TERMINAL, sellerTerminalId));

        assertThat(response.ready()).isTrue();
        assertThat(response.notifications()).isEmpty();
        assertThat(response.badges()).isEmpty();
        assertThat(response.attentionLevel()).isEqualTo(PosAttentionLevel.NONE);
    }

    private SellerTerminalView sellerTerminal(SellerTerminalStatus status) {
        return new SellerTerminalView(
            sellerTerminalId,
            tenantId,
            "ST-001",
            "Seller Terminal",
            "Seller",
            "Terminal",
            "st@test.me",
            "+15145550100",
            null,
            status,
            BigDecimal.ZERO,
            null,
            Instant.parse("2026-05-21T12:00:00Z"),
            null,
            null,
            null,
            null, false, null);
    }

    private PosAvailableDrawView primaryDraw() {
        return new PosAvailableDrawView(
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

    private TchRequestContext context(TchActorType actorType, SellerTerminalId terminalId) {
        return new TchRequestContext(
            "tenant-demo",
            tenantId.value(),
            "tenant-demo",
            tenantId.value(),
            UUID.randomUUID().toString(),
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
