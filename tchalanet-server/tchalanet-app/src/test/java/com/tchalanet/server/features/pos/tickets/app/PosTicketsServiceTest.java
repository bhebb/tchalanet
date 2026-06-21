package com.tchalanet.server.features.pos.tickets.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;
import com.tchalanet.server.core.sales.api.model.verification.TicketCashierVerificationView;
import com.tchalanet.server.core.sales.api.query.GetTicketForCashierVerificationQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.features.pos.tickets.mapper.PosTicketMapper;
import com.tchalanet.server.features.pos.tickets.model.PosAction;
import com.tchalanet.server.features.pos.tickets.model.PosActionType;
import com.tchalanet.server.features.pos.tickets.model.PosVerifyTicketRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PosTicketsServiceTest {

    private final QueryBus queryBus = mock(QueryBus.class);
    private final CommandBus commandBus = mock(CommandBus.class);
    private final PosTicketMapper mapper = mock(PosTicketMapper.class);
    private final TicketPrintReaderPort port = mock(TicketPrintReaderPort.class);
    private final TicketScanResolver ticketScanResolver = new TicketScanResolver();

    private final PosTicketsService service = new PosTicketsService(
        queryBus, commandBus, mapper, ticketScanResolver, port);

    private final TenantId tenantId = TenantId.of(UUID.randomUUID());
    private final UserId userId = UserId.of(UUID.randomUUID());
    private final TicketId ticketId = TicketId.of(UUID.randomUUID());
    private final DrawId drawId = DrawId.of(UUID.randomUUID());

    @Nested
    @DisplayName("scan normalization")
    class ScanNormalization {

        @Test
        @DisplayName("raw public code is passed as-is to the query")
        void raw_public_code_resolves_ticket() {
            stubTicket(CustomerTicketStatus.AWAITING_RESULT, null, null);

            var response = service.verify(trustedContext(), new PosVerifyTicketRequest("TCH8F4K29PL"));

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_RESULT_PENDING");
        }

        @Test
        @DisplayName("full public URL is normalised to the public code")
        void full_url_resolves_ticket() {
            stubTicket(CustomerTicketStatus.AWAITING_RESULT, null, null);

            var response = service.verify(
                trustedContext(), new PosVerifyTicketRequest("https://tchalanet.com/v/TCH-8F4K-29PL"));

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_RESULT_PENDING");
        }
    }

    @Nested
    @DisplayName("ticket statuses")
    class TicketStatuses {

        @Test
        @DisplayName("AWAITING_RESULT with draw in future → NOT_PAYABLE_PENDING_DRAW")
        void awaiting_result_future_draw_returns_pending_draw() {
            stubTicket(CustomerTicketStatus.AWAITING_RESULT, null, Instant.now().plusSeconds(3600));

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_PENDING_DRAW");
        }

        @Test
        @DisplayName("AWAITING_RESULT with past draw → NOT_PAYABLE_RESULT_PENDING")
        void awaiting_result_past_draw_returns_result_pending() {
            stubTicket(CustomerTicketStatus.AWAITING_RESULT, null, Instant.now().minusSeconds(3600));

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_RESULT_PENDING");
        }

        @Test
        @DisplayName("LOST → NOT_PAYABLE_LOST")
        void lost_ticket_returns_not_payable_lost() {
            stubTicket(CustomerTicketStatus.LOST, null, null);

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_LOST");
        }

        @Test
        @DisplayName("WON_CLAIMABLE → PAYABLE without payout workflow")
        void won_claimable_returns_payable_without_payout_action() {
            stubTicket(CustomerTicketStatus.WON_CLAIMABLE, winAmount(), null);

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("PAYABLE");
            assertThat(response.availableActions())
                .extracting(PosAction::type)
                .doesNotContain(PosActionType.EXECUTE_PAYOUT);
        }

        @Test
        @DisplayName("WON_PAID → ALREADY_PAID without EXECUTE_PAYOUT")
        void won_paid_returns_already_paid_without_execute_action() {
            stubTicket(CustomerTicketStatus.WON_PAID, winAmount(), null);

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("ALREADY_PAID");
            assertThat(response.availableActions())
                .extracting(PosAction::type)
                .doesNotContain(PosActionType.EXECUTE_PAYOUT);
        }
        @Test
        @DisplayName("ticket not found → NOT_FOUND")
        void ticket_not_found_returns_not_found() {
            when(queryBus.ask(any(GetTicketForCashierVerificationQuery.class)))
                .thenThrow(ProblemRestException.notFound("ticket.not_found"));

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("seller-terminal context guard")
    class SellerTerminalContextGuard {

        @Test
        @DisplayName("missing seller-terminal → OPERATION_NOT_ALLOWED")
        void null_context_returns_operation_not_allowed() {
            var response = service.verify(contextWithoutSellerTerminal(), request());

            assertThat(response.status()).isEqualTo("OPERATION_NOT_ALLOWED");
        }
    }

    // --- stubs ---

    private void stubTicket(
        CustomerTicketStatus customerStatus,
        Money winningAmount,
        Instant drawScheduledAt
    ) {
        var view = new TicketCashierVerificationView(
            ticketId,
            "TCH-CODE-01",
            "TCH8F4K29PL",
            "TCH-8F4K-29PL",
            customerStatus,
            TicketSaleStatus.APPROVED,
            TicketResultStatus.PENDING,
            TicketSettlementStatus.NOT_SETTLED,
            new Money(BigDecimal.valueOf(100), CurrencyCode.of("HTG")),
            winningAmount,
            Instant.parse("2026-05-28T10:00:00Z"),
            drawId,
            drawScheduledAt
        );
        when(queryBus.ask(any(GetTicketForCashierVerificationQuery.class))).thenReturn(view);
    }

    private Money winAmount() {
        return new Money(BigDecimal.valueOf(500), CurrencyCode.of("HTG"));
    }

    private PosVerifyTicketRequest request() {
        return new PosVerifyTicketRequest("TCH8F4K29PL");
    }

    private TchRequestContext trustedContext() {
        return contextWith(SellerTerminalId.of(UUID.randomUUID()));
    }

    private TchRequestContext contextWithoutSellerTerminal() {
        return contextWith(null);
    }

    private TchRequestContext contextWith(SellerTerminalId sellerTerminalId) {
        return new TchRequestContext(
            "tenant-demo", tenantId.value(), "tenant-demo", tenantId.value(),
            UUID.randomUUID().toString(), userId.value(),
            Set.of(), Set.of(),
            Locale.FRANCE, "req-test", "127.0.0.1", null, false, null, "active",
            ApiScope.TENANT, null, tenantId,
            java.time.ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            null,
            TchActorType.SELLER_TERMINAL,
            sellerTerminalId,
            Set.of(),
            Set.of("ticket.read_own"),
            null
        );
    }
}
