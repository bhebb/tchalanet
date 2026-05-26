package com.tchalanet.server.core.sales.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.command.print.RecordTicketPrintCommand;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintStateStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.internal.application.command.handler.lifecycle.ApproveTicketSaleCommandHandler;
import com.tchalanet.server.core.sales.internal.application.command.handler.lifecycle.CancelTicketCommandHandler;
import com.tchalanet.server.core.sales.internal.application.command.handler.print.RecordTicketPrintCommandHandler;
import com.tchalanet.server.core.sales.internal.application.command.model.ApproveTicketSaleCommand;
import com.tchalanet.server.core.sales.internal.application.command.model.CancelTicketCommand;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.print.TicketPrintPolicyService;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Ticket command mutation coverage")
class TicketCommandMutationCoverageTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final Instant NOW = Instant.parse("2026-05-21T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final TenantId TENANT = TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final UserId USER = UserId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    @AfterEach
    void clearContext() {
        TchContext.clear();
    }

    @Test
    @DisplayName("RecordTicketPrintCommand passes print mutation and preserves immutable fields")
    void recordPrintMutation() {
        setContext();
        var original = approvedTicket();
        var writer = new CapturingWriter();
        var handler = new RecordTicketPrintCommandHandler(
            new SingleTicketReader(original),
            writer,
            new CapturingPublisher(),
            CLOCK,
            fixedIds(),
            new TicketPrintPolicyService());

        handler.handle(new RecordTicketPrintCommand(
            original.identity().id(),
            PrintOutputFormat.PDF,
            null));

        assertImmutableTicketFields(writer.saved, original);
        assertThat(writer.saved.print().status()).isEqualTo(TicketPrintStateStatus.PRINTED);
        assertThat(writer.saved.print().printCount()).isEqualTo(1);
        assertThat(writer.saved.print().firstPrintedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("ApproveTicketSaleCommand passes approval mutation and preserves immutable fields")
    void approveMutation() {
        setContext();
        var original = pendingTicket();
        var writer = new CapturingWriter();
        var handler = new ApproveTicketSaleCommandHandler(
            new SingleTicketReader(original),
            writer,
            new CapturingPublisher(),
            CLOCK,
            fixedIds());

        handler.handle(new ApproveTicketSaleCommand(
            TENANT,
            original.identity().id(),
            USER,
            "approved"));

        assertImmutableTicketFields(writer.saved, original);
        assertThat(writer.saved.lifecycle().sale().status()).isEqualTo(TicketSaleStatus.APPROVED);
        assertThat(writer.saved.lifecycle().sale().approval().approvedBy()).isEqualTo(USER);
        assertThat(writer.saved.lifecycle().sale().approval().approvedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("CancelTicketCommand passes cancellation mutation and preserves immutable fields")
    void cancelMutation() {
        setContext();
        var original = approvedTicket();
        var writer = new CapturingWriter();
        var handler = new CancelTicketCommandHandler(
            new SingleTicketReader(original),
            writer,
            new CapturingPublisher(),
            CLOCK,
            fixedIds());

        handler.handle(new CancelTicketCommand(
            TENANT,
            original.identity().id(),
            USER,
            "customer requested cancellation"));

        assertImmutableTicketFields(writer.saved, original);
        assertThat(writer.saved.lifecycle().sale().status()).isEqualTo(TicketSaleStatus.CANCELLED);
        assertThat(writer.saved.lifecycle().sale().cancellation().by()).isEqualTo(USER);
        assertThat(writer.saved.lifecycle().sale().cancellation().at()).isEqualTo(NOW);
        assertThat(writer.saved.lifecycle().sale().cancellation().reason())
            .isEqualTo("customer requested cancellation");
    }

    @Test
    @DisplayName("Record print then cancel keeps immutable fields across multiple command updates")
    void printThenCancelSequence() {
        setContext();
        var original = approvedTicket();
        var store = new MutableTicketStore(original);
        var publisher = new CapturingPublisher();

        var printHandler = new RecordTicketPrintCommandHandler(
            store,
            store,
            publisher,
            CLOCK,
            fixedIds(),
            new TicketPrintPolicyService());
        var cancelHandler = new CancelTicketCommandHandler(
            store,
            store,
            publisher,
            CLOCK,
            fixedIds());

        printHandler.handle(new RecordTicketPrintCommand(
            original.identity().id(),
            PrintOutputFormat.PDF,
            null));
        cancelHandler.handle(new CancelTicketCommand(
            TENANT,
            original.identity().id(),
            USER,
            "customer requested cancellation"));

        assertImmutableTicketFields(store.ticket, original);
        assertThat(store.ticket.print().status()).isEqualTo(TicketPrintStateStatus.PRINTED);
        assertThat(store.ticket.lifecycle().sale().status()).isEqualTo(TicketSaleStatus.CANCELLED);
        assertThat(store.ticket.lifecycle().sale().cancellation().reason())
            .isEqualTo("customer requested cancellation");
    }

    private static void assertImmutableTicketFields(Ticket actual, Ticket original) {
        assertThat(actual.identity()).isEqualTo(original.identity());
        assertThat(actual.context()).isEqualTo(original.context());
        assertThat(actual.codes()).isEqualTo(original.codes());
        assertThat(actual.origin()).isEqualTo(original.origin());
        assertThat(actual.money().currency()).isEqualTo(original.money().currency());
        assertThat(actual.audit().createdAt()).isEqualTo(original.audit().createdAt());
        assertThat(actual.audit().createdBy()).isEqualTo(original.audit().createdBy());
    }

    private static void setContext() {
        TchContext.set(TchRequestContext.startupTenant(TENANT.value(), "test").withAppUserId(USER.value()));
    }

    private static IdGenerator fixedIds() {
        return () -> UUID.fromString("01000000-0000-0000-0000-000000000001");
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
            NOW.minusSeconds(60));
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
            NOW.minusSeconds(60));
    }

    private static TicketIdentity identity() {
        return new TicketIdentity(
            TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
            TENANT);
    }

    private static TicketContext context() {
        return new TicketContext(
            OutletId.of(UUID.fromString("50000000-0000-0000-0000-000000000001")),
            TerminalId.of(UUID.fromString("60000000-0000-0000-0000-000000000001")),
            USER,
            SalesSessionId.of(UUID.fromString("70000000-0000-0000-0000-000000000001")),
            DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001")),
            DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001")),
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
            TicketLineResultStatus.PENDING,
            money("0"));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }

    private static class SingleTicketReader implements TicketReaderPort {
        private final Ticket ticket;

        private SingleTicketReader(Ticket ticket) {
            this.ticket = ticket;
        }

        @Override public Optional<Ticket> findById(TicketId ticketId) { return Optional.of(ticket); }
        @Override public Ticket getRequired(TicketId ticketId) { return ticket; }
        @Override public Optional<Ticket> findByTicketCode(String ticketCode) { return Optional.empty(); }
        @Override public Optional<Ticket> findByPublicCode(String publicCode) { return Optional.empty(); }
        @Override public Optional<Ticket> findByVerificationCode(String verificationCode) { return Optional.empty(); }
        @Override public Optional<Ticket> findByOfflineSubmissionId(OfflineSubmissionId submissionId) { return Optional.empty(); }
        @Override public List<Ticket> findByDrawId(DrawId drawId) { return List.of(); }
        @Override public boolean existsByOfflineSubmissionId(OfflineSubmissionId submissionId) { return false; }
    }

    private static class CapturingWriter implements TicketWriterPort {
        private Ticket saved;

        @Override
        public Ticket save(Ticket ticket) {
            this.saved = ticket;
            return ticket;
        }

        @Override public void flushPending() {}
    }

    private static class MutableTicketStore implements TicketReaderPort, TicketWriterPort {
        private Ticket ticket;

        private MutableTicketStore(Ticket ticket) {
            this.ticket = ticket;
        }

        @Override public Optional<Ticket> findById(TicketId ticketId) { return Optional.of(ticket); }
        @Override public Ticket getRequired(TicketId ticketId) { return ticket; }
        @Override public Optional<Ticket> findByTicketCode(String ticketCode) { return Optional.empty(); }
        @Override public Optional<Ticket> findByPublicCode(String publicCode) { return Optional.empty(); }
        @Override public Optional<Ticket> findByVerificationCode(String verificationCode) { return Optional.empty(); }
        @Override public Optional<Ticket> findByOfflineSubmissionId(OfflineSubmissionId submissionId) { return Optional.empty(); }
        @Override public List<Ticket> findByDrawId(DrawId drawId) { return List.of(); }
        @Override public boolean existsByOfflineSubmissionId(OfflineSubmissionId submissionId) { return false; }

        @Override
        public Ticket save(Ticket ticket) {
            this.ticket = ticket;
            return ticket;
        }

        @Override public void flushPending() {}
    }

    private static class CapturingPublisher implements DomainEventPublisher {
        private final List<DomainEvent> events = new ArrayList<>();

        @Override public void publish(DomainEvent event) { events.add(event); }
        @Override public void publish(Collection<? extends DomainEvent> events) { this.events.addAll(events); }
    }
}
