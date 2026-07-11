package com.tchalanet.server.core.analytics.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSelectionRepository;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnalyticsSelectionProjectorTest {

  private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final DrawChannelId DRAW_CHANNEL_ID =
      DrawChannelId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final LocalDate REF_DATE = LocalDate.parse("2026-06-25");
  private static final Instant NOW = Instant.parse("2026-06-25T10:00:00Z");

  @Test
  void keepsMaryajGratisIdentifiableWithoutIncreasingPaidStake() {
    var repository = mock(AnalyticsSelectionRepository.class);
    var projector = new AnalyticsSelectionProjector(repository);

    projector.applyTicketPlaced(ticketPlaced(), REF_DATE);

    var gameCodeCaptor = ArgumentCaptor.forClass(String.class);
    var stakeCaptor = ArgumentCaptor.forClass(Long.class);
    verify(repository, org.mockito.Mockito.times(2)).upsertAndIncrement(
        org.mockito.Mockito.eq(TENANT_ID.value()),
        org.mockito.Mockito.eq(REF_DATE),
        org.mockito.Mockito.eq(DRAW_CHANNEL_ID.value()),
        gameCodeCaptor.capture(),
        org.mockito.Mockito.anyString(),
        org.mockito.Mockito.any(),
        org.mockito.Mockito.anyString(),
        org.mockito.Mockito.eq(1L),
        stakeCaptor.capture(),
        org.mockito.Mockito.eq(0L));

    assertThat(gameCodeCaptor.getAllValues()).containsExactly(
        GameCode.HT_BOLET.name(),
        GameCode.HT_MARYAJ_GRATIS.name());
    assertThat(stakeCaptor.getAllValues()).containsExactly(1_000L, 0L);
  }

  private static TicketPlacedEvent ticketPlaced() {
    return new TicketPlacedEvent(
        EventId.of(UUID.fromString("30000000-0000-0000-0000-000000000001")),
        TicketPlacedEvent.CURRENT_SCHEMA,
        NOW,
        CorrelationId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
        TENANT_ID,
        TicketId.of(UUID.fromString("50000000-0000-0000-0000-000000000001")),
        TicketSaleStatus.APPROVED,
        TicketSaleChannel.WEB,
        new TicketContextPayload(
            null,
            DRAW_CHANNEL_ID,
            SellerTerminalId.of(UUID.fromString("60000000-0000-0000-0000-000000000001")),
            null,
            null),
        new TicketMoneyPayload(HTG, money("10.00"), money("10.00"), List.of()),
        List.of(
            line(
                1,
                GameCode.HT_BOLET,
                BetType.MATCH_2_2D,
                "12",
                null,
                TicketLineOrigin.CUSTOMER,
                money("10.00"),
                null),
            line(
                2,
                GameCode.HT_MARYAJ_GRATIS,
                BetType.MARRIAGE_2D2D,
                "12-34",
                (short) 2,
                TicketLineOrigin.PROMOTION,
                money("0.00"),
                PromotionDecisionId.of(UUID.fromString("70000000-0000-0000-0000-000000000001")))),
        null);
  }

  private static TicketLinePlacedItem line(
      int lineNumber,
      GameCode gameCode,
      BetType betType,
      String selection,
      Short betOption,
      TicketLineOrigin origin,
      Money stake,
      PromotionDecisionId promotionDecisionId) {
    return new TicketLinePlacedItem(
        TicketLineId.of(UUID.fromString("80000000-0000-0000-0000-00000000000" + lineNumber)),
        lineNumber,
        gameCode,
        betType,
        selection,
        selection,
        betOption,
        stake,
        origin,
        origin == TicketLineOrigin.PROMOTION ? TicketLinePricingSource.PROMOTION : TicketLinePricingSource.STANDARD,
        origin == TicketLineOrigin.PROMOTION
            ? TicketLineSelectionSource.PROMOTION_GENERATED
            : TicketLineSelectionSource.CUSTOMER_SELECTED,
        promotionDecisionId,
        null,
        origin == TicketLineOrigin.PROMOTION ? "FREE_GAME_LINE" : null);
  }

  private static Money money(String amount) {
    return new Money(new BigDecimal(amount), HTG);
  }
}
