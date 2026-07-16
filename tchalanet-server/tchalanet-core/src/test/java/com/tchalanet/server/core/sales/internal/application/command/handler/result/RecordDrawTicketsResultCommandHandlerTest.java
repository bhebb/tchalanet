package com.tchalanet.server.core.sales.internal.application.command.handler.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultProjection;
import com.tchalanet.server.core.sales.api.command.result.RecordDrawTicketsResultCommand;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidEvent;
import com.tchalanet.server.core.sales.api.event.TicketResultedEvent;
import com.tchalanet.server.core.sales.api.model.line.TicketLineResult;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.result.TicketWinningCalculator;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.infra.cache.SalesTicketCacheEvictor;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NoOpCacheManager;

class RecordDrawTicketsResultCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
  private static final DrawId DRAW_ID =
      DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001"));
  private static final DrawChannelId DRAW_CHANNEL_ID =
      DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001"));
  private static final SellerTerminalId SELLER_TERMINAL_ID =
      SellerTerminalId.of(UUID.fromString("91000000-0000-0000-0000-000000000001"));
  private static final DrawResultId DRAW_RESULT_ID =
      DrawResultId.of(UUID.fromString("50000000-0000-0000-0000-000000000001"));
  private static final Instant NOW = Instant.parse("2026-01-02T22:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void drawWithNoTicketsSettlesToZeroTicketUpdates() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var drawId = DrawId.of(UUID.randomUUID());
    var drawResultId = DrawResultId.of(UUID.randomUUID());
    var handler =
        new RecordDrawTicketsResultCommandHandler(
            new EmptyTicketReaderPort(),
            new FailingTicketWriterPort(),
            new ProjectionQueryBus(drawResultId),
            new TicketWinningCalculator(),
            new FailingEventPublisher(),
            new SalesTicketCacheEvictor(new NoOpCacheManager()),
            () -> UUID.randomUUID(),
            CLOCK);

    var result =
        handler.handle(
            new RecordDrawTicketsResultCommand(
                tenantId,
                drawId,
                drawResultId,
                LocalDate.of(2026, 1, 2),
                ResultSlotId.of(UUID.randomUUID()),
                DrawChannelId.of(UUID.randomUUID())));

    assertThat(result.processedTickets()).isZero();
    assertThat(result.updatedTickets()).isZero();
    assertThat(result.skippedTickets()).isZero();
  }

  @Test
  void winningTicketIsSettledPaidAndPublishesSingleFinancialEvent() {
    var store = new CapturingTicketStore(ticket("45"));
    var publisher = new CapturingEventPublisher();
    var handler = handler(store, publisher);

    var result = handler.handle(command());

    assertThat(result.processedTickets()).isEqualTo(1);
    assertThat(result.updatedTickets()).isEqualTo(1);
    assertThat(result.skippedTickets()).isZero();

    var saved = store.savedTickets().getFirst();
    assertThat(saved.lifecycle().result().status()).isEqualTo(TicketResultStatus.WON);
    assertThat(saved.lifecycle().settlement().status()).isEqualTo(TicketSettlementStatus.PAID);
    assertThat(saved.winningAmount().amount()).isEqualByComparingTo("125.00");
    assertThat(saved.lines().getFirst().payoutAmount().amount()).isEqualByComparingTo("125.00");

    assertThat(publisher.events()).filteredOn(TicketResultedEvent.class::isInstance).hasSize(1);
    assertThat(publisher.events())
        .filteredOn(TicketPayoutPaidEvent.class::isInstance)
        .hasSize(1)
        .first()
        .satisfies(
            event -> assertThat(((TicketPayoutPaidEvent) event).amountCents()).isEqualTo(12_500L));
  }

  @Test
  void losingTicketIsSettledWithoutPayoutAndPublishesNoFinancialSettlementEvents() {
    var store = new CapturingTicketStore(ticket("12"));
    var publisher = new CapturingEventPublisher();
    var handler = handler(store, publisher);

    var result = handler.handle(command());

    assertThat(result.processedTickets()).isEqualTo(1);
    assertThat(result.updatedTickets()).isEqualTo(1);
    assertThat(result.skippedTickets()).isZero();

    var saved = store.savedTickets().getFirst();
    assertThat(saved.lifecycle().result().status()).isEqualTo(TicketResultStatus.LOST);
    assertThat(saved.lifecycle().settlement().status()).isEqualTo(TicketSettlementStatus.NO_PAYOUT);
    assertThat(saved.winningAmount().amount()).isEqualByComparingTo("0.00");
    assertThat(saved.lines().getFirst().payoutAmount().amount()).isEqualByComparingTo("0.00");

    assertThat(publisher.events()).filteredOn(TicketResultedEvent.class::isInstance).hasSize(1);
    assertThat(publisher.events())
        .noneMatch(TicketPayoutPaidEvent.class::isInstance);
  }

  @Test
  void settlementDataErrorSkipsTicketWithoutSavingLostResult() {
    var store = new CapturingTicketStore(ticket("45"));
    var publisher = new CapturingEventPublisher();
    var handler =
        new RecordDrawTicketsResultCommandHandler(
            store,
            store,
            new ProjectionQueryBus(DRAW_RESULT_ID),
            new ThrowingTicketWinningCalculator(),
            publisher,
            new SalesTicketCacheEvictor(new NoOpCacheManager()),
            () -> UUID.fromString("01000000-0000-0000-0000-000000000001"),
            CLOCK);

    var result = handler.handle(command());

    assertThat(result.processedTickets()).isEqualTo(1);
    assertThat(result.updatedTickets()).isZero();
    assertThat(result.skippedTickets()).isEqualTo(1);
    assertThat(store.savedTickets()).isEmpty();
    assertThat(publisher.events()).isEmpty();
  }

  @Test
  void replaySkipsAlreadyResultedTicketWithoutSavingOrPublishingAgain() {
    var store = new CapturingTicketStore(resultedWinningTicket());
    var publisher = new CapturingEventPublisher();
    var handler = handler(store, publisher);

    var result = handler.handle(command());

    assertThat(result.processedTickets()).isEqualTo(1);
    assertThat(result.updatedTickets()).isZero();
    assertThat(result.skippedTickets()).isEqualTo(1);
    assertThat(store.savedTickets()).isEmpty();
    assertThat(publisher.events()).isEmpty();
  }

  private static RecordDrawTicketsResultCommandHandler handler(
      CapturingTicketStore store, DomainEventPublisher publisher) {
    return new RecordDrawTicketsResultCommandHandler(
        store,
        store,
        new ProjectionQueryBus(DRAW_RESULT_ID),
        new TicketWinningCalculator(),
        publisher,
        new SalesTicketCacheEvictor(new NoOpCacheManager()),
        () -> UUID.fromString("01000000-0000-0000-0000-000000000001"),
        CLOCK);
  }

  private static RecordDrawTicketsResultCommand command() {
    return new RecordDrawTicketsResultCommand(
        TENANT_ID,
        DRAW_ID,
        DRAW_RESULT_ID,
        LocalDate.of(2026, 1, 2),
        ResultSlotId.of(UUID.fromString("60000000-0000-0000-0000-000000000001")),
        DRAW_CHANNEL_ID);
  }

  private static Ticket ticket(String selection) {
    var stake = money("10.00");
    return Ticket.place(
        new TicketIdentity(
            TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")), TENANT_ID),
        new TicketContext(DRAW_ID, DRAW_CHANNEL_ID, SELLER_TERMINAL_ID, null, null),
        TicketCodes.from("TCK-123456-123456-ABC123-4", "ABCD-1234", "WXYZ-5678"),
        new TicketMoneyBreakdown(stake, List.of(), stake),
        List.of(
            TicketLine.customerLine(
                TicketLineId.of(UUID.fromString("41000000-0000-0000-0000-000000000001")),
                1,
                GameCode.HT_BOLET,
                BetType.MATCH_2_2D,
                new Selection(SelectionKey.of(selection), selection),
                stake,
                new BigDecimal("12.5000"),
                null,
                TicketLineResultStatus.PENDING,
                money("0.00"))),
        TicketSaleChannel.POS_ONLINE,
        false,
        null,
        UserId.of(UUID.fromString("10000000-0000-0000-0000-000000000001")),
        NOW.minusSeconds(60));
  }

  private static Ticket resultedWinningTicket() {
    var actor = UserId.of(UUID.fromString("10000000-0000-0000-0000-000000000002"));
    return ticket("45")
        .applyOfficialResult(
            Map.of(
                TicketLineId.of(UUID.fromString("41000000-0000-0000-0000-000000000001")),
                new TicketLineResult(TicketLineResultStatus.WON, money("125.00"))),
            actor,
            NOW)
        .autoSettleAfterResult(actor, NOW);
  }

  private static Money money(String amount) {
    return new Money(new BigDecimal(amount), CurrencyCode.of("HTG"));
  }

  private record ProjectionQueryBus(DrawResultId drawResultId) implements QueryBus {
    @Override
    @SuppressWarnings("unchecked")
    public <R> R ask(Query<R> query) {
      return (R)
          new DrawResultProjection(
              drawResultId,
              "MIDDAY",
              Instant.parse("2026-01-02T22:00:00Z"),
              "123",
              "45",
              "67",
              null,
              List.of("45", "67"));
    }
  }

  private static final class CapturingTicketStore implements TicketReaderPort, TicketWriterPort {
    private final Ticket ticket;
    private final List<Ticket> savedTickets = new ArrayList<>();

    private CapturingTicketStore(Ticket ticket) {
      this.ticket = ticket;
    }

    @Override
    public Optional<Ticket> findById(TicketId ticketId) {
      return Optional.of(ticket);
    }

    @Override
    public Ticket getRequired(TicketId ticketId) {
      return ticket;
    }

    @Override
    public Optional<Ticket> findByTicketCode(String ticketCode) {
      return Optional.empty();
    }

    @Override
    public Optional<Ticket> findByPublicCode(String publicCode) {
      return Optional.empty();
    }

    @Override
    public Optional<Ticket> findByVerificationCode(String verificationCode) {
      return Optional.empty();
    }

    @Override
    public List<Ticket> findByDrawId(DrawId drawId) {
      return List.of(ticket);
    }

    @Override
    public Ticket save(Ticket ticket) {
      savedTickets.add(ticket);
      return ticket;
    }

    @Override
    public void flushPending() {}

    private List<Ticket> savedTickets() {
      return savedTickets;
    }
  }

  private static final class CapturingEventPublisher implements DomainEventPublisher {
    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
      events.add(event);
    }

    @Override
    public void publish(Collection<? extends DomainEvent> events) {
      this.events.addAll(events);
    }

    private List<DomainEvent> events() {
      return events;
    }
  }

  private static final class EmptyTicketReaderPort implements TicketReaderPort {
    @Override
    public Optional<Ticket> findById(TicketId ticketId) {
      return Optional.empty();
    }

    @Override
    public Ticket getRequired(TicketId ticketId) {
      throw new AssertionError("No ticket expected");
    }

    @Override
    public Optional<Ticket> findByTicketCode(String ticketCode) {
      return Optional.empty();
    }

    @Override
    public Optional<Ticket> findByPublicCode(String publicCode) {
      return Optional.empty();
    }

    @Override
    public Optional<Ticket> findByVerificationCode(String verificationCode) {
      return Optional.empty();
    }

    @Override
    public List<Ticket> findByDrawId(DrawId drawId) {
      return List.of();
    }
  }

  private static final class FailingTicketWriterPort implements TicketWriterPort {
    @Override
    public Ticket save(Ticket ticket) {
      throw new AssertionError("No ticket should be saved");
    }

    @Override
    public void flushPending() {
      throw new AssertionError("No flush expected");
    }
  }

  private static final class FailingEventPublisher implements DomainEventPublisher {
    @Override
    public void publish(DomainEvent event) {
      throw new AssertionError("No event expected");
    }

    @Override
    public void publish(Collection<? extends DomainEvent> events) {
      if (!events.isEmpty()) {
        throw new AssertionError("No event expected");
      }
    }
  }

  private static final class ThrowingTicketWinningCalculator extends TicketWinningCalculator {
    @Override
    public java.util.HashMap<
            TicketLineId, com.tchalanet.server.core.sales.api.model.line.TicketLineResult>
        computeLineResults(Ticket ticket, DrawResultProjection projection) {
      throw new IllegalStateException("settlement.terms_snapshot_required");
    }
  }
}
