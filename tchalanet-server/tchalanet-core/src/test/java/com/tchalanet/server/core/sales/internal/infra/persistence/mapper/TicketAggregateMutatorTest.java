package com.tchalanet.server.core.sales.internal.infra.persistence.mapper;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.line.TicketLineResult;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintStateStatus;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketAggregateMutator")
class TicketAggregateMutatorTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final Instant NOW = Instant.parse("2026-05-21T10:00:00Z");
    private static final UserId USER = UserId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final TenantId TENANT = TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final TicketJpaMapper MAPPER = new TicketJpaMapper() {
    };
    private final TicketAggregateMutator mutator = new TicketAggregateMutator(MAPPER);

    @Test
    @DisplayName("copies print mutation and preserves immutable fields")
    void printMutation() {
        var original = approvedTicket();
        var managed = MAPPER.toEntity(original);

        var printed = original.markPrinted(USER, NOW.plusSeconds(60));

        mutator.applyTo(managed, printed);

        assertImmutableTicketFields(managed, original);
        assertThat(managed.getPrintStatus()).isEqualTo(TicketPrintStateStatus.PRINTED);
        assertThat(managed.getPrintCount()).isEqualTo(1);
        assertThat(managed.getFirstPrintedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(managed.getLastPrintedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    @DisplayName("copies approval transition and preserves immutable fields")
    void approvalMutation() {
        var original = pendingTicket();
        var managed = MAPPER.toEntity(original);

        var approved = original.approve(USER, "approved", NOW.plusSeconds(60));

        mutator.applyTo(managed, approved);

        assertImmutableTicketFields(managed, original);
        assertThat(managed.getSaleStatus()).isEqualTo(TicketSaleStatus.APPROVED);
        assertThat(managed.getApprovedBy()).isEqualTo(USER.value());
        assertThat(managed.getApprovedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    @DisplayName("copies line result fields without replacing immutable line data")
    void lineResultMutation() {
        var original = approvedTicket();
        var managed = MAPPER.toEntity(original);
        var payout = new Money(new BigDecimal("125"), HTG);
        var resulted = original.applyOfficialResult(
            Map.of(original.lines().getFirst().id(), new TicketLineResult(TicketLineResultStatus.WON, payout)),
            USER,
            NOW.plusSeconds(60));

        mutator.applyTo(managed, resulted);

        assertImmutableTicketFields(managed, original);
        assertThat(managed.getResultStatus()).isEqualTo(TicketResultStatus.WON);
        assertThat(managed.getWinningAmount()).isEqualByComparingTo("125");
        assertThat(managed.getLines()).hasSize(1);
        var line = managed.getLines().getFirst();
        assertThat(line.getResultStatus()).isEqualTo(TicketLineResultStatus.WON);
        assertThat(line.getPayoutAmount()).isEqualByComparingTo("125");
        assertThat(line.getSelectionKey()).isEqualTo("05");
    }

    @Test
    @DisplayName("adds a new line without replacing existing line data")
    void addsLineOnUpdate() {
        var original = approvedTicket();
        var managed = MAPPER.toEntity(original);
        var withAddedLine = withLines(original, List.of(line(), secondLine()));

        mutator.applyTo(managed, withAddedLine);

        assertImmutableTicketFields(managed, original);
        assertThat(managed.getLines()).hasSize(2);
        assertThat(managed.getLines())
            .extracting(com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketLineJpaEntity::getSelectionKey)
            .containsExactly("05", "07");
        assertThat(managed.getLines().get(1).getTicket()).isSameAs(managed);
    }

    @Test
    @DisplayName("removes orphaned lines from the managed aggregate")
    void removesLineOnUpdate() {
        var original = withLines(approvedTicket(), List.of(line(), secondLine()));
        var managed = MAPPER.toEntity(original);
        var withRemovedLine = withLines(original, List.of(line()));

        mutator.applyTo(managed, withRemovedLine);

        assertImmutableTicketFields(managed, original);
        assertThat(managed.getLines()).hasSize(1);
        assertThat(managed.getLines().getFirst().getSelectionKey()).isEqualTo("05");
    }

    @Test
    @DisplayName("rejects immutable tenant changes")
    void rejectsImmutableTenantChange() {
        var original = approvedTicket();
        var managed = MAPPER.toEntity(original);
        var changedTenant = new Ticket(
            new TicketIdentity(TicketId.of(original.identity().id().value()), TenantId.of(UUID.randomUUID())),
            original.context(),
            original.codes(),
            original.money(),
            original.lifecycle(),
            original.origin(),
            original.print(),
            original.audit(),
            original.lines());

        assertThatThrownBy(() -> mutator.applyTo(managed, changedTenant))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("tenantId");
    }

    private static void assertImmutableTicketFields(
        com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity managed,
        Ticket original
    ) {
        assertThat(managed.getId()).isEqualTo(original.identity().id().value());
        assertThat(managed.getTenantId()).isEqualTo(original.identity().tenantId().value());
        assertThat(managed.getDrawId()).isEqualTo(original.context().drawId().value());
        assertThat(managed.getDrawChannelId()).isEqualTo(original.context().drawChannelId().value());
        assertThat(managed.getTicketCode()).isEqualTo(original.codes().ticketCode().value());
        assertThat(managed.getPublicCode()).isEqualTo(original.codes().publicCode().value());
        assertThat(managed.getVerificationCode()).isEqualTo(original.codes().verificationCode().value());
        assertThat(managed.getCurrency()).isEqualTo(original.money().currency().value());
    }

    private static Ticket pendingTicket() {
        return Ticket.place(
            identity(),
            context(),
            TicketCodes.from("TCK-123456-123456-ABC123-4", "ABCD-1234", "WXYZ-5678"),
            new TicketMoneyBreakdown(money("10"), List.of(), money("10")),
            List.of(line()),
            TicketSaleChannel.POS_ONLINE,
            null,
            true,
            ApprovalRequestId.of(UUID.fromString("30000000-0000-0000-0000-000000000001")),
            USER,
            NOW);
    }

    private static Ticket approvedTicket() {
        return Ticket.place(
            identity(),
            context(),
            TicketCodes.from("TCK-123456-123456-ABC123-4", "ABCD-1234", "WXYZ-5678"),
            new TicketMoneyBreakdown(money("10"), List.of(), money("10")),
            List.of(line()),
            TicketSaleChannel.POS_ONLINE,
            null,
            false,
            null,
            USER,
            NOW);
    }

    private static TicketIdentity identity() {
        return new TicketIdentity(
            TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
            TENANT);
    }

    private static TicketContext context() {
        return new TicketContext(
            DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001")),
            DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001")),
            SellerTerminalId.of(UUID.fromString("91000000-0000-0000-0000-000000000001")),
            null,
            null);
    }

    private static TicketLine line() {
        return new TicketLine(
            TicketLineId.of(UUID.fromString("41000000-0000-0000-0000-000000000001")),
            1,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            new Selection(SelectionKey.of("05"), "05"),
            money("10"),
            money("125"),
            new BigDecimal("12.5"),
            money("125"),
            null,
            TicketLineOrigin.CUSTOMER,
            TicketLinePricingSource.STANDARD,
            null,
            null,
            null,
            null,
            TicketLineResultStatus.PENDING,
            money("0"));
    }

    private static TicketLine secondLine() {
        return new TicketLine(
            TicketLineId.of(UUID.fromString("42000000-0000-0000-0000-000000000001")),
            2,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            new Selection(SelectionKey.of("07"), "07"),
            money("0"),
            money("0"),
            new BigDecimal("12.5"),
            money("0"),
            null,
            TicketLineOrigin.CUSTOMER,
            TicketLinePricingSource.STANDARD,
            null,
            null,
            null,
            null,
            TicketLineResultStatus.PENDING,
            money("0"));
    }

    private static Ticket withLines(Ticket ticket, List<TicketLine> lines) {
        return new Ticket(
            ticket.identity(),
            ticket.context(),
            ticket.codes(),
            ticket.money(),
            ticket.lifecycle(),
            ticket.origin(),
            ticket.print(),
            ticket.audit(),
            lines);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }
}
