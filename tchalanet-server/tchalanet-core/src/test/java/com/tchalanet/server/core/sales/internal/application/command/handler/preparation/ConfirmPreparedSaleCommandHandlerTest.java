package com.tchalanet.server.core.sales.internal.application.command.handler.preparation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.sales.api.command.preparation.ConfirmPreparedSaleCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketResult;
import com.tchalanet.server.core.sales.api.command.sell.SoldTicketView;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.internal.application.service.preparation.SalePreparationInputCodec;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationPromotionLine;
import com.tchalanet.server.core.sales.internal.domain.service.preparation.SalePreparationStateMachine;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConfirmPreparedSaleCommandHandler")
class ConfirmPreparedSaleCommandHandlerTest {

  private static final Instant NOW = Instant.parse("2026-06-10T12:00:00Z");
  private static final UUID PREP_ID = UUID.randomUUID();
  private static final UUID TICKET_ID = UUID.randomUUID();

  private final InMemorySalePreparationStore store = new InMemorySalePreparationStore();

  private static class CapturingBus implements CommandBus {
    SellTicketCommand captured;
    SellTicketResult result;

    @Override
    @SuppressWarnings("unchecked")
    public <R> R execute(Command<R> command) {
      this.captured = (SellTicketCommand) command;
      return (R) result;
    }
  }

  private final CapturingBus bus = new CapturingBus();

  private ConfirmPreparedSaleCommandHandler handler() {
    return new ConfirmPreparedSaleCommandHandler(
        store,
        new SalePreparationInputCodec(),
        new SalePreparationStateMachine(),
        bus,
        new TimeProvider(Clock.fixed(NOW, ZoneOffset.UTC)));
  }

  private SalePreparation draft(Instant expiresAt) {
    return new SalePreparation(
        PREP_ID,
        SalePreparationStatus.DRAFT,
        SellerTerminalId.of(UUID.randomUUID()),
        UUID.randomUUID(),
        "hash",
        Map.of(
            "drawId", UUID.randomUUID().toString(),
            "drawChannelId", UUID.randomUUID().toString(),
            "currency", "HTG",
            "lines",
                List.of(
                    Map.of(
                        "lineNumber", 1,
                        "gameCode", "HT_BOLET",
                        "betType", "MATCH_1_2D",
                        "rawSelection", "12",
                        "stakeAmount", "25"))),
        null,
        null,
        null,
        expiresAt,
        null,
        List.of(
            new SalePreparationPromotionLine(
                "ref-1",
                "HT_MARYAJ_GRATIS",
                "MARRIAGE_2D2D",
                (short) 1,
                "34-78",
                TicketLineSelectionSource.PROMOTION_GENERATED,
                PromotionChoiceMode.AUTO_GENERATE,
                null,
                null,
                "maryaj-gratis",
                "FREE_GAME_LINE",
                "ctx-hash",
                "engine-v1",
                true,
                3,
                1)));
  }

  private static SellTicketResult soldResult() {
    var ticket =
        new SoldTicketView(
            TicketId.of(TICKET_ID),
            "TCK-1",
            "PUB-1",
            "PUB-1",
            null,
            null,
            null,
            null,
            null,
            null,
            SellerTerminalId.of(UUID.randomUUID()),
            null,
            null,
            NOW,
            NOW);
    return new SellTicketResult(ticket, SellTicketOutcome.ACCEPTED, List.of());
  }

  @Test
  @DisplayName("confirms: pins stored selections and persists exactly the prepared lines")
  void happyPathPinsSelections() {
    store.create(draft(NOW.plusSeconds(60)));
    bus.result = soldResult();

    var out = handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-1"));

    assertThat(out.alreadyConfirmed()).isFalse();
    assertThat(out.ticketId()).isEqualTo(TICKET_ID);
    assertThat(store.byId.get(PREP_ID).status()).isEqualTo(SalePreparationStatus.CONFIRMED);
    assertThat(store.byId.get(PREP_ID).idempotencyKey()).isEqualTo("idem-1");

    var choices = bus.captured.promotionChoices();
    assertThat(choices).hasSize(1);
    assertThat(choices.get(0).rawSelection()).isEqualTo("34-78");
    assertThat(choices.get(0).gameCode()).isEqualTo("HT_MARYAJ_GRATIS");
    assertThat(choices.get(0).selectionSource())
        .isEqualTo(TicketLineSelectionSource.PROMOTION_GENERATED);
    assertThat(bus.captured.lines()).hasSize(1);
    assertThat(bus.captured.lines().get(0).rawSelection()).isEqualTo("12");
  }

  @Test
  @DisplayName("confirm keeps the stored manual selection source")
  void confirmKeepsStoredManualSelectionSource() {
    var preparation = draft(NOW.plusSeconds(60));
    var manualLine = preparation.promotionLines().get(0);
    store.create(
        new SalePreparation(
            preparation.id(),
            preparation.status(),
            preparation.sellerTerminalId(),
            preparation.drawId(),
            preparation.inputHash(),
            preparation.input(),
            preparation.promotionDecisionId(),
            preparation.idempotencyKey(),
            preparation.ticketId(),
            preparation.expiresAt(),
            preparation.confirmedAt(),
            List.of(
                new SalePreparationPromotionLine(
                    manualLine.lineRef(),
                    manualLine.gameCode(),
                    manualLine.betType(),
                    manualLine.betOption(),
                    manualLine.selection(),
                    TicketLineSelectionSource.CUSTOMER_SELECTED,
                    PromotionChoiceMode.SELLER_SELECTS,
                    manualLine.promotionDecisionId(),
                    manualLine.promotionRuleId(),
                    manualLine.promotionRuleKey(),
                    manualLine.promotionEffectType(),
                    manualLine.promotionDecisionContextHash(),
                    manualLine.promotionDecisionEngineVersion(),
                    manualLine.regenerable(),
                    manualLine.maxRegenerations(),
                    manualLine.regenerationCount()))));
    bus.result = soldResult();

    handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-1"));

    assertThat(bus.captured.promotionChoices().get(0).selectionSource())
        .isEqualTo(TicketLineSelectionSource.CUSTOMER_SELECTED);
  }

  @Test
  @DisplayName("confirm pins every stored promotion choice with stable per-game indexes")
  void confirmPinsPromotionChoicesWithPerGameIndexes() {
    var preparation = draft(NOW.plusSeconds(60));
    var first = preparation.promotionLines().get(0);
    store.create(
        new SalePreparation(
            preparation.id(),
            preparation.status(),
            preparation.sellerTerminalId(),
            preparation.drawId(),
            preparation.inputHash(),
            preparation.input(),
            preparation.promotionDecisionId(),
            preparation.idempotencyKey(),
            preparation.ticketId(),
            preparation.expiresAt(),
            preparation.confirmedAt(),
            List.of(
                first,
                new SalePreparationPromotionLine(
                    "ref-2",
                    "HT_MARYAJ_GRATIS",
                    "MARRIAGE_2D2D",
                    (short) 1,
                    "11-22",
                    TicketLineSelectionSource.PROMOTION_GENERATED,
                    PromotionChoiceMode.AUTO_GENERATE,
                    null,
                    null,
                    "maryaj-gratis",
                    "FREE_GAME_LINE",
                    "ctx-hash",
                    "engine-v1",
                    true,
                    3,
                    1),
                new SalePreparationPromotionLine(
                    "ref-3",
                    "HT_BOLET",
                    "MATCH_1_2D",
                    null,
                    "99",
                    TicketLineSelectionSource.CUSTOMER_SELECTED,
                    PromotionChoiceMode.SELLER_SELECTS,
                    null,
                    null,
                    "bolet-free",
                    "FREE_GAME_LINE",
                    "ctx-hash",
                    "engine-v1",
                    false,
                    0,
                    0))));
    bus.result = soldResult();

    handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-1"));

    assertThat(bus.captured.promotionChoices())
        .extracting(
            choice -> choice.gameCode(),
            choice -> choice.index(),
            choice -> choice.rawSelection(),
            choice -> choice.selectionSource(),
            choice -> choice.decisionId())
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                "HT_MARYAJ_GRATIS",
                0,
                "34-78",
                TicketLineSelectionSource.PROMOTION_GENERATED,
                null),
            org.assertj.core.groups.Tuple.tuple(
                "HT_MARYAJ_GRATIS",
                1,
                "11-22",
                TicketLineSelectionSource.PROMOTION_GENERATED,
                null),
            org.assertj.core.groups.Tuple.tuple(
                "HT_BOLET", 0, "99", TicketLineSelectionSource.CUSTOMER_SELECTED, null));
  }

  @Test
  @DisplayName("double confirm with the same idempotencyKey returns the same ticket")
  void idempotentReplay() {
    store.create(draft(NOW.plusSeconds(60)));
    bus.result = soldResult();
    handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-1"));

    var replay = handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-1"));

    assertThat(replay.alreadyConfirmed()).isTrue();
    assertThat(replay.ticketId()).isEqualTo(TICKET_ID);
    assertThat(replay.sale()).isNull();
  }

  @Test
  @DisplayName("confirm with a different idempotencyKey on a confirmed preparation is rejected")
  void differentKeyRejected() {
    store.create(draft(NOW.plusSeconds(60)));
    bus.result = soldResult();
    handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-1"));

    assertThatThrownBy(() -> handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-2")))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.preparation.already_confirmed");
  }

  @Test
  @DisplayName("expired preparation is rejected and marked EXPIRED")
  void expiredRejected() {
    store.create(draft(NOW.minusSeconds(1)));

    assertThatThrownBy(() -> handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-1")))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.preparation.expired");
    assertThat(store.byId.get(PREP_ID).status()).isEqualTo(SalePreparationStatus.EXPIRED);
  }

  @Test
  @DisplayName("rejected sale leaves the preparation DRAFT")
  void rejectedSaleKeepsDraft() {
    store.create(draft(NOW.plusSeconds(60)));
    bus.result = new SellTicketResult(null, SellTicketOutcome.REJECTED, List.of());

    var out = handler().handle(new ConfirmPreparedSaleCommand(PREP_ID, "idem-1"));

    assertThat(out.ticketId()).isNull();
    assertThat(out.sale().outcome()).isEqualTo(SellTicketOutcome.REJECTED);
    assertThat(store.byId.get(PREP_ID).status()).isEqualTo(SalePreparationStatus.DRAFT);
  }
}
