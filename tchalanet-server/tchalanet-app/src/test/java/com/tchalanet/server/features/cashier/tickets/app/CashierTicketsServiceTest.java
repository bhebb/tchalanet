package com.tchalanet.server.features.cashier.tickets.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.payout.api.query.ListPayoutsQuery;
import com.tchalanet.server.core.payout.api.query.PayoutRow;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;
import com.tchalanet.server.core.sales.api.model.verification.TicketCashierVerificationView;
import com.tchalanet.server.core.sales.api.query.GetTicketForCashierVerificationQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextResolver;
import com.tchalanet.server.features.cashier.tickets.mapper.CashierTicketMapper;
import com.tchalanet.server.features.cashier.tickets.model.CashierAction;
import com.tchalanet.server.features.cashier.tickets.model.CashierActionType;
import com.tchalanet.server.features.cashier.tickets.model.CashierVerifyTicketRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CashierTicketsServiceTest {

    private final QueryBus queryBus = mock(QueryBus.class);
    private final CommandBus commandBus = mock(CommandBus.class);
    private final SellerOperationalContextResolver sellerContextResolver =
        mock(SellerOperationalContextResolver.class);
    private final CashierTicketMapper mapper = mock(CashierTicketMapper.class);
    private final TicketPrintReaderPort port = mock(TicketPrintReaderPort.class);
    private final TicketScanResolver ticketScanResolver = new TicketScanResolver();

    private final CashierTicketsService service = new CashierTicketsService(
        queryBus, commandBus, sellerContextResolver, mapper, ticketScanResolver, port);

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
            stubEmptyPayouts();

            var response = service.verify(trustedContext(), new CashierVerifyTicketRequest("TCH8F4K29PL"));

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_RESULT_PENDING");
        }

        @Test
        @DisplayName("full public URL is normalised to the public code")
        void full_url_resolves_ticket() {
            stubTicket(CustomerTicketStatus.AWAITING_RESULT, null, null);
            stubEmptyPayouts();

            var response = service.verify(
                trustedContext(), new CashierVerifyTicketRequest("https://tchalanet.com/v/TCH-8F4K-29PL"));

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
            stubEmptyPayouts();

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_PENDING_DRAW");
        }

        @Test
        @DisplayName("AWAITING_RESULT with past draw → NOT_PAYABLE_RESULT_PENDING")
        void awaiting_result_past_draw_returns_result_pending() {
            stubTicket(CustomerTicketStatus.AWAITING_RESULT, null, Instant.now().minusSeconds(3600));
            stubEmptyPayouts();

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_RESULT_PENDING");
        }

        @Test
        @DisplayName("LOST → NOT_PAYABLE_LOST")
        void lost_ticket_returns_not_payable_lost() {
            stubTicket(CustomerTicketStatus.LOST, null, null);
            stubEmptyPayouts();

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("NOT_PAYABLE_LOST");
        }

        @Test
        @DisplayName("WON_CLAIMABLE with OPEN payout → PAYABLE with EXECUTE_PAYOUT action")
        void won_claimable_open_payout_returns_payable_with_execute_action() {
            stubTicket(CustomerTicketStatus.WON_CLAIMABLE, winAmount(), null);
            stubPayout(PayoutClaimStatus.OPEN);

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("PAYABLE");
            assertThat(response.availableActions())
                .extracting(CashierAction::type)
                .contains(CashierActionType.EXECUTE_PAYOUT);
        }

        @Test
        @DisplayName("WON_PAID → ALREADY_PAID without EXECUTE_PAYOUT")
        void won_paid_returns_already_paid_without_execute_action() {
            stubTicket(CustomerTicketStatus.WON_PAID, winAmount(), null);
            stubEmptyPayouts();

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("ALREADY_PAID");
            assertThat(response.availableActions())
                .extracting(CashierAction::type)
                .doesNotContain(CashierActionType.EXECUTE_PAYOUT);
        }

        @Test
        @DisplayName("WON_CLAIMABLE with BLOCKED payout → BLOCKED")
        void won_claimable_blocked_payout_returns_blocked() {
            stubTicket(CustomerTicketStatus.WON_CLAIMABLE, winAmount(), null);
            stubPayout(PayoutClaimStatus.BLOCKED);

            var response = service.verify(trustedContext(), request());

            assertThat(response.status()).isEqualTo("BLOCKED");
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
    @DisplayName("operational context guard")
    class OperationalContextGuard {

        @Test
        @DisplayName("null context → OPERATION_NOT_ALLOWED")
        void null_context_returns_operation_not_allowed() {
            var response = service.verify(contextWith(null), request());

            assertThat(response.status()).isEqualTo("OPERATION_NOT_ALLOWED");
        }

        @Test
        @DisplayName("WEAK trust → OPERATION_NOT_ALLOWED")
        void weak_trust_returns_operation_not_allowed() {
            var response = service.verify(contextWith(hint(OperationalContextTrust.WEAK)), request());

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

    private void stubEmptyPayouts() {
        when(queryBus.ask(any(ListPayoutsQuery.class)))
            .thenReturn(TchPage.of(List.of(), 0, 20, 0, 0, true, false, false));
    }

    private void stubPayout(PayoutClaimStatus status) {
        var payout = new PayoutRow(
            PayoutId.of(UUID.randomUUID()),
            ticketId,
            BigDecimal.valueOf(500),
            status,
            Instant.parse("2026-05-28T10:00:00Z"),
            OutletId.of(UUID.randomUUID()),
            "PDV Test"
        );
        when(queryBus.ask(any(ListPayoutsQuery.class)))
            .thenReturn(TchPage.of(List.of(payout), 0, 20, 1, 1, true, false, false));
    }

    private Money winAmount() {
        return new Money(BigDecimal.valueOf(500), CurrencyCode.of("HTG"));
    }

    private CashierVerifyTicketRequest request() {
        return new CashierVerifyTicketRequest("TCH8F4K29PL");
    }

    private OperationalContextHint hint(OperationalContextTrust trust) {
        return new OperationalContextHint(
            TerminalId.of(UUID.randomUUID()),
            OutletId.of(UUID.randomUUID()),
            SalesSessionId.of(UUID.randomUUID()),
            OperationalContextSource.SIGNED_DEVICE_BINDING,
            trust
        );
    }

    private TchRequestContext trustedContext() {
        return contextWith(hint(OperationalContextTrust.STRONG));
    }

    private TchRequestContext contextWith(OperationalContextHint hint) {
        return new TchRequestContext(
            "tenant-demo", tenantId.value(), "tenant-demo", tenantId.value(),
            UUID.randomUUID().toString(), userId.value(),
            Set.of(TchRole.CASHIER), Set.of(),
            Locale.FRANCE, "req-test", "127.0.0.1", null, false, null, "active",
            ApiScope.TENANT, null, tenantId,
            java.time.ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            hint
        );
    }
}
