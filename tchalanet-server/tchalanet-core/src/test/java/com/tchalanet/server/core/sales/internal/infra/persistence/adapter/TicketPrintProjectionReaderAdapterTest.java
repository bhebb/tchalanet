package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.config.TicketPublicProperties;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintStateStatus;
import com.tchalanet.server.core.sales.api.model.settlement.PayoutRuleSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementRuleCode;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSource;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermsSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementWinMode;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketPrintStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketVerificationUrlBuilder;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.infra.persistence.mapper.TicketJpaMapper;
import com.tchalanet.server.core.sales.internal.infra.persistence.mapper.TicketPrintViewMapper;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketPrintHeaderViewRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.view.TicketPrintHeaderViewEntity;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketPrintProjectionReaderAdapterTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final Instant NOW = Instant.parse("2026-07-06T15:00:00Z");
    private static final TenantId TENANT = TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final TicketId TICKET_ID = TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001"));
    private static final DrawId DRAW_ID = DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private static final DrawChannelId DRAW_CHANNEL_ID =
        DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final SellerTerminalId TERMINAL_ID =
        SellerTerminalId.of(UUID.fromString("91000000-0000-0000-0000-000000000001"));
    private static final UserId USER = UserId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    @Test
    void reprintProjectionLoadsCustomerSafeLineSnapshot() {
        var headerRepository = mock(TicketPrintHeaderViewRepository.class);
        var ticketRepository = mock(TicketJpaRepository.class);
        var mapper = new TicketJpaMapper() {};
        var printMapper = new TicketPrintViewMapper(
            new TicketVerificationUrlBuilder(new TicketPublicProperties("https://tickets.test", "/check/{code}")));
        var adapter = new TicketPrintProjectionReaderAdapter(
            headerRepository,
            ticketRepository,
            mapper,
            printMapper);
        var ticket = ticketWithCoverageLine();
        var header = header();

        when(headerRepository.getRequired(TICKET_ID.value())).thenReturn(header);
        when(ticketRepository.findById(TICKET_ID.value())).thenReturn(Optional.of(mapper.toEntity(ticket)));

        var printView = adapter.findPrintViewRequired(TICKET_ID);

        assertThat(printView.lines()).hasSize(1);
        var line = printView.lines().getFirst();
        assertThat(line.selectionCanonical()).isEqualTo("123");
        assertThat(line.betOptionLabel()).isEqualTo("Exact + Permuté");
        assertThat(line.selectionPolicySnapshot()).isNotNull();
        assertThat(printView.printState().status()).isEqualTo(TicketPrintStateStatus.NOT_PRINTED);
    }

    private static TicketPrintHeaderViewEntity header() {
        var header = mock(TicketPrintHeaderViewEntity.class);
        when(header.getTenantId()).thenReturn(TENANT.value());
        when(header.getTicketCode()).thenReturn("TCK-123456-123456-ABC123-4");
        when(header.getPublicCode()).thenReturn("ABCD-1234");
        when(header.getVerificationCode()).thenReturn("WXYZ-5678");
        when(header.getSaleStatus()).thenReturn(TicketSaleStatus.APPROVED);
        when(header.getResultStatus()).thenReturn(TicketResultStatus.NOT_RESULTED);
        when(header.getSettlementStatus()).thenReturn(TicketSettlementStatus.NOT_SETTLED);
        when(header.getPrintStatus()).thenReturn(TicketPrintStatus.NOT_PRINTED);
        when(header.getPrintCount()).thenReturn(0);
        when(header.getDrawId()).thenReturn(DRAW_ID.value());
        when(header.getDrawDate()).thenReturn(LocalDate.parse("2026-07-06"));
        when(header.getScheduledAt()).thenReturn(NOW.plusSeconds(3600));
        when(header.getCutoffAt()).thenReturn(NOW.plusSeconds(3300));
        when(header.getDrawChannelId()).thenReturn(DRAW_CHANNEL_ID.value());
        when(header.getDrawChannelCode()).thenReturn("NY");
        when(header.getDrawChannelName()).thenReturn("New York");
        when(header.getDrawChannelDisplayName()).thenReturn("New York Midi");
        when(header.getResultSlotKey()).thenReturn("MIDDAY");
        when(header.getResultProvider()).thenReturn("provider-1");
        when(header.getResultTimezone()).thenReturn("America/Port-au-Prince");
        when(header.getSellerTerminalId()).thenReturn(TERMINAL_ID.value());
        when(header.getSellerTerminalCode()).thenReturn("TERM-1");
        when(header.getSellerTerminalLabel()).thenReturn("Terminal 1");
        when(header.getSellerDisplayName()).thenReturn("Seller 1");
        when(header.getTenantDisplayName()).thenReturn("Tenant Demo");
        when(header.getStakeAmount()).thenReturn(new BigDecimal("20"));
        when(header.getTotalAmount()).thenReturn(new BigDecimal("20"));
        when(header.getCurrency()).thenReturn(HTG.value());
        when(header.getPlacedAt()).thenReturn(NOW);
        when(header.getSaleOrigin()).thenReturn(TicketSaleChannel.POS_ONLINE);
        when(header.getLocale()).thenReturn("fr");
        when(header.getTimezone()).thenReturn("America/Port-au-Prince");
        return header;
    }

    private static Ticket ticketWithCoverageLine() {
        return Ticket.place(
            new TicketIdentity(TICKET_ID, TENANT),
            new TicketContext(DRAW_ID, DRAW_CHANNEL_ID, TERMINAL_ID, null, null),
            TicketCodes.from("TCK-123456-123456-ABC123-4", "ABCD-1234", "WXYZ-5678"),
            new TicketMoneyBreakdown(money("20"), List.of(), money("20")),
            List.of(multiTermLine()),
            TicketSaleChannel.POS_ONLINE,
            false,
            null,
            USER,
            NOW);
    }

    private static TicketLine multiTermLine() {
        return new TicketLine(
            TicketLineId.of(UUID.fromString("43000000-0000-0000-0000-000000000001")),
            1,
            GameCode.HT_LOTO3,
            BetType.LOTTO3_3D,
            new Selection(SelectionKey.of("123"), "123"),
            money("20"),
            SettlementTermsSnapshot.current(
                SelectionPolicy.EXPLICIT_ONLY,
                List.of(
                    settlementTerm(SettlementRuleCode.LOTTO3_STRAIGHT, new BigDecimal("500")),
                    settlementTerm(SettlementRuleCode.LOTTO3_BOX_6_WAY, new BigDecimal("80")))),
            (short) 3,
            SelectionPolicy.EXPLICIT_ONLY,
            "Exact + Permuté",
            TicketLineOrigin.CUSTOMER,
            TicketLinePricingSource.STANDARD,
            null,
            null,
            null,
            null,
            TicketLineResultStatus.PENDING,
            money("0"));
    }

    private static SettlementTermSnapshot settlementTerm(SettlementRuleCode ruleCode, BigDecimal multiplier) {
        return new SettlementTermSnapshot(
            ruleCode,
            (short) 3,
            "Exact + Permuté",
            PayoutRuleSnapshot.stakeMultiplier(multiplier),
            new BigDecimal("10"),
            SettlementWinMode.ALTERNATIVE,
            SettlementTermSource.TENANT_DEFAULT);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }
}
