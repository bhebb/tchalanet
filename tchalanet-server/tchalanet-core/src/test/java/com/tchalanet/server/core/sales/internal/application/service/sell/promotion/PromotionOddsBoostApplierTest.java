package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionDecisionStatus;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Money path: promotional odds boost. Boosts ONLY the lines whose gameCode matches
 * the effect, marking them as PROMOTION-priced, and is a no-op for other effect
 * types. Currently only exercised via the Spring integration test.
 */
@DisplayName("PromotionOddsBoostApplier")
class PromotionOddsBoostApplierTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final PromotionRuleId RULE_ID = PromotionRuleId.of(UUID.randomUUID());
    private static final PromotionDecisionId DECISION_ID = PromotionDecisionId.of(UUID.randomUUID());

    private final PromotionOddsBoostApplier applier = new PromotionOddsBoostApplier();

    private static TicketLine boletLine() {
        return TicketLine.customerLine(
            TicketLineId.of(UUID.randomUUID()),
            1,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            new Selection(SelectionKey.of("05"), "05"),
            new Money(new BigDecimal("10"), HTG),
            new BigDecimal("50.0000"),
            null,
            TicketLineResultStatus.PENDING,
            null);
    }

    private static PromotionEffect boost(String gameCode, BigDecimal amount) {
        return new PromotionEffect(
            RULE_ID, PromotionEffectType.BOOST_ODDS, gameCode, 1, amount, "HTG", null, "boost",
            PromotionChoiceMode.NONE);
    }

    private static PromotionDecision decision(PromotionEffect effect) {
        return new PromotionDecision(
            DECISION_ID, PromotionDecisionStatus.APPLIED, PromotionEvaluationPhase.SALE_CONFIRMATION,
            Instant.parse("2026-07-09T12:00:00Z"), "ctx", "v1", List.of(effect), List.of());
    }

    @Nested
    @DisplayName("BOOST_ODDS targeting by gameCode")
    class Targeting {

        @Test
        @DisplayName("boosts only lines whose gameCode matches the effect")
        void boostsMatchingGame() {
            var lines = new ArrayList<>(List.of(boletLine()));
            var effect = boost("HT_BOLET", new BigDecimal("2.5000"));

            applier.apply(lines, effect, decision(effect));

            var line = lines.get(0);
            assertThat(line.pricingSource()).isEqualTo(TicketLinePricingSource.PROMOTION);
            assertThat(line.promotionDecisionId()).isEqualTo(DECISION_ID);
            assertThat(line.promotionEffectType()).isEqualTo("BOOST_ODDS");
        }

        @Test
        @DisplayName("leaves lines of a different gameCode untouched")
        void leavesOtherGameUntouched() {
            var lines = new ArrayList<>(List.of(boletLine()));
            var effect = boost("HT_LOTO5", new BigDecimal("2.5000"));

            applier.apply(lines, effect, decision(effect));

            var line = lines.get(0);
            assertThat(line.pricingSource()).isEqualTo(TicketLinePricingSource.STANDARD);
            assertThat(line.promotionDecisionId()).isNull();
        }
    }

    @Nested
    @DisplayName("guards")
    class Guards {

        @Test
        @DisplayName("non-BOOST_ODDS effect is a strict no-op")
        void nonBoostIsNoOp() {
            var lines = new ArrayList<>(List.of(boletLine()));
            var effect = new PromotionEffect(
                RULE_ID, PromotionEffectType.WAIVE_CHARGE, "HT_BOLET", 1, new BigDecimal("2.5"), "HTG",
                "BUYER_SMS", "x", PromotionChoiceMode.NONE);

            applier.apply(lines, effect, decision(effect));

            assertThat(lines.get(0).pricingSource()).isEqualTo(TicketLinePricingSource.STANDARD);
        }

        @Test
        @DisplayName("missing gameCode is rejected")
        void gameCodeRequired() {
            var lines = new ArrayList<>(List.of(boletLine()));
            var effect = boost(null, new BigDecimal("2.5"));

            assertThatThrownBy(() -> applier.apply(lines, effect, decision(effect)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boost_odds_game_required");
        }

        @Test
        @DisplayName("non-positive amount is rejected")
        void amountRequired() {
            var lines = new ArrayList<>(List.of(boletLine()));
            var zero = boost("HT_BOLET", BigDecimal.ZERO);

            assertThatThrownBy(() -> applier.apply(lines, zero, decision(zero)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boost_odds_amount_required");
        }
    }

    @Nested
    @DisplayName("scale handling")
    class Scale {

        // Fragility note (test-plan §5): setScale(4, RoundingMode.UNNECESSARY) means a
        // boost amount with >4 decimals throws ArithmeticException DURING the sale, rather
        // than being rejected at promotion-config time. Pinned here so it's a deliberate
        // contract, not a surprise.
        @Test
        @DisplayName("a boost amount with more than 4 decimals throws (RoundingMode.UNNECESSARY)")
        void moreThanFourDecimalsThrows() {
            var lines = new ArrayList<>(List.of(boletLine()));
            var effect = boost("HT_BOLET", new BigDecimal("2.12345"));

            assertThatThrownBy(() -> applier.apply(lines, effect, decision(effect)))
                .isInstanceOf(ArithmeticException.class);
        }
    }
}
