package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.catalog.pricing.internal.web.model.PricingOddsView;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionDecisionStatus;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.PromotionChoiceInput;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.application.service.sell.generation.DefaultSelectionGenerationService;
import com.tchalanet.server.core.sales.internal.application.service.sell.generation.RandomSelectionGenerator;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.internal.application.DefaultSelectionApi;
import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import com.tchalanet.server.core.selection.api.model.SelectionValidationResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Sale promotion effects")
class SalePromotionEffectsTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final Instant NOW = Instant.parse("2026-05-21T10:00:00Z");

    private final PromotionChargeApplier chargeApplier = new PromotionChargeApplier();
    private final PromotionOddsBoostApplier oddsBoostApplier = new PromotionOddsBoostApplier();
    private static final SelectionApi SELECTION_STUB = new SelectionApi() {
        @Override
        public Selection canonicalize(BetType betType, Short betOption, String rawSelection) {
            return new Selection(SelectionKey.of(rawSelection), rawSelection);
        }
        @Override
        public Selection canonicalize(BetType betType, String rawSelection) {
            return new Selection(SelectionKey.of(rawSelection), rawSelection);
        }
        @Override
        public SelectionValidationResult validate(BetType betType, Short betOption, String rawSelection) {
            return SelectionValidationResult.valid(canonicalize(betType, betOption, rawSelection));
        }
        @Override
        public SelectionValidationResult validate(BetType betType, String rawSelection) {
            return SelectionValidationResult.valid(canonicalize(betType, rawSelection));
        }
    };

    private static final PricingCatalog PRICING_STUB = new PricingCatalog() {
        @Override
        public BigDecimal oddsFor(TenantId tenantId, String gameCode, BetType betType, Short betOption) {
            return new BigDecimal("12.5");
        }

        @Override
        public List<PricingOddsView> getOdds(TenantId tenantId) {
            return List.of();
        }

        @Override
        public com.tchalanet.server.catalog.pricing.api.model.PricingStatsView stats() {
            return null;
        }
    };

    private final PromotionTicketLineFactory lineFactory = new PromotionTicketLineFactory(
        () -> UUID.fromString("99000000-0000-0000-0000-000000000001"),
        SELECTION_STUB,
        PRICING_STUB,
        new PromotionSelectionResolver(new DefaultSelectionGenerationService(
            new RandomSelectionGenerator(),
            new DefaultSelectionApi()
        ))
    );
    private final SalePromotionEffectApplier applier =
        new SalePromotionEffectApplier(lineFactory, oddsBoostApplier, chargeApplier);

    // -------------------------------------------------------------------------
    // WAIVE_CHARGE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("WAIVE_CHARGE")
    class WaiveCharge {

        @Test
        @DisplayName("removes matching charge from the list")
        void removesMatchingCharge() {
            var smsCharge = new TicketCharge(TicketChargeType.BUYER_SMS, money("5"), ChargePaidBy.BUYER);
            var waivableEffect = effect(PromotionEffectType.WAIVE_CHARGE, null, "BUYER_SMS", null);
            var decision = decision(waivableEffect);

            var result = applier.apply(decision, List.of(customerLine()), new ArrayList<>(List.of(smsCharge)), null, HTG);

            assertThat(result.charges()).hasSize(1);
            var waived = result.charges().get(0);
            assertThat(waived.isWaived()).isTrue();
            assertThat(waived.waivedByDecisionId()).isEqualTo(decision.decisionId());
            assertThat(waived.waivedByRuleId()).isEqualTo(waivableEffect.ruleId());
            assertThat(waived.waivedEffectType()).isEqualTo(PromotionEffectType.WAIVE_CHARGE.name());
            assertThat(waived.amount()).isEqualTo(money("5"));
            assertThat(result.ticketLines()).hasSize(1);
        }

        @Test
        @DisplayName("preserves charges for different type")
        void preservesOtherCharges() {
            var whatsappCharge = new TicketCharge(TicketChargeType.BUYER_WHATSAPP, money("3"), ChargePaidBy.BUYER);
            var waiveSmsEffect = effect(PromotionEffectType.WAIVE_CHARGE, null, "BUYER_SMS", null);
            var decision = decision(waiveSmsEffect);

            var result = applier.apply(decision, List.of(customerLine()), new ArrayList<>(List.of(whatsappCharge)), null, HTG);

            assertThat(result.charges()).hasSize(1);
            assertThat(result.charges().get(0).type()).isEqualTo(TicketChargeType.BUYER_WHATSAPP);
        }
    }

    // -------------------------------------------------------------------------
    // BOOST_ODDS
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("BOOST_ODDS")
    class BoostOdds {

        @Test
        @DisplayName("updates odds snapshot and potential payout on matching game line")
        void boostsMatchingLine() {
            var line = customerLine(); // HT_BOLET, stakeAmount=10, odds=12.5
            var boostEffect = effect(PromotionEffectType.BOOST_ODDS, "HT_BOLET", null, new BigDecimal("20.0000"));
            var decision = decision(boostEffect);

            var result = applier.apply(decision, new ArrayList<>(List.of(line)), List.of(), null, HTG);

            assertThat(result.ticketLines()).hasSize(1);
            var boosted = result.ticketLines().get(0);
            assertThat(boosted.oddsSnapshot()).isEqualByComparingTo("20.0000");
            assertThat(boosted.pricingSource()).isEqualTo(TicketLinePricingSource.PROMOTION);
            assertThat(boosted.promotionDecisionId()).isEqualTo(decision.decisionId());
            assertThat(boosted.promotionLabel()).isEqualTo(TicketReceiptI18nKeys.PROMOTION_BOOST_ODDS);
        }

        @Test
        @DisplayName("does not affect lines for a different game code")
        void ignoresOtherGameCode() {
            var line = customerLine(); // HT_BOLET
            var boostEffect = effect(PromotionEffectType.BOOST_ODDS, "HT_MEGA_LOT", null, new BigDecimal("20.0000"));
            var decision = decision(boostEffect);

            var result = applier.apply(decision, new ArrayList<>(List.of(line)), List.of(), null, HTG);

            var unchanged = result.ticketLines().get(0);
            assertThat(unchanged.oddsSnapshot()).isEqualByComparingTo("12.5");
            assertThat(unchanged.pricingSource()).isEqualTo(TicketLinePricingSource.STANDARD);
            assertThat(unchanged.promotionDecisionId()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // FREE_GAME_LINE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("FREE_GAME_LINE")
    class FreeGameLine {

        @Test
        @DisplayName("adds a promotional line with stake=0, origin=PROMOTION, payoutBase from effect")
        void addsPromotionalLine() {
            var freeEffect = effect(PromotionEffectType.FREE_GAME_LINE, "HT_BOLET", null, new BigDecimal("125"));
            // quantity defaults to 0 in helper — need a quantity=1 effect
            var freeEffectQ1 = new PromotionEffect(
                PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001")),
                PromotionEffectType.FREE_GAME_LINE,
                "HT_BOLET",
                1,
                new BigDecimal("125"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.NONE
            );
            var decision = decision(freeEffectQ1);
            var paid = List.of(customerLine());

            var result = applier.apply(decision, new ArrayList<>(paid), List.of(), command(), HTG);

            assertThat(result.ticketLines()).hasSize(2);
            var promoLine = result.ticketLines().get(1);
            assertThat(promoLine.origin()).isEqualTo(TicketLineOrigin.PROMOTION);
            assertThat(promoLine.pricingSource()).isEqualTo(TicketLinePricingSource.PROMOTION);
            assertThat(promoLine.stakeAmount()).isEqualTo(Money.zero(HTG));
            assertThat(promoLine.payoutBaseAmount().amount()).isEqualByComparingTo("125");
            assertThat(promoLine.oddsSnapshot()).isEqualByComparingTo("12.5");
            assertThat(promoLine.promotionDecisionId()).isEqualTo(decision.decisionId());
            assertThat(promoLine.promotionLabel()).isEqualTo(TicketReceiptI18nKeys.PROMOTION_FREE_GAME_LINE);
        }

        @Test
        @DisplayName("promotional line has line number after last customer line")
        void promotionLineNumberFollowsCustomerLines() {
            var freeEffectQ1 = new PromotionEffect(
                PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001")),
                PromotionEffectType.FREE_GAME_LINE,
                "HT_BOLET",
                1,
                new BigDecimal("125"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.NONE
            );
            var decision = decision(freeEffectQ1);

            var result = applier.apply(decision, new ArrayList<>(List.of(customerLine())), List.of(), command(), HTG);

            var promoLine = result.ticketLines().get(1);
            assertThat(promoLine.lineNumber()).isGreaterThan(customerLine().lineNumber());
        }

        @Test
        @DisplayName("requires explicit customer selection when configured")
        void requiresExplicitSelectionWhenConfigured() {
            var freeEffect = new PromotionEffect(
                PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001")),
                PromotionEffectType.FREE_GAME_LINE,
                "HT_BOLET",
                1,
                new BigDecimal("125"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.CUSTOMER_SELECTS
            );

            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                applier.apply(decision(freeEffect), new ArrayList<>(List.of(customerLine())), List.of(), command(), HTG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("promotion.free_game_selection_required");
        }

        @Test
        @DisplayName("uses customer-selected promotion choice when provided")
        void usesCustomerSelectedPromotionChoice() {
            var freeEffect = new PromotionEffect(
                PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001")),
                PromotionEffectType.FREE_GAME_LINE,
                "HT_BOLET",
                1,
                new BigDecimal("125"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.CUSTOMER_SELECTS
            );
            var decision = decision(freeEffect);
            var result = applier.apply(
                decision,
                new ArrayList<>(List.of(customerLine())),
                List.of(),
                command(List.of(new PromotionChoiceInput(
                    decision.decisionId(),
                    "HT_BOLET",
                    0,
                    "77",
                    TicketLineSelectionSource.CUSTOMER_SELECTED
                ))),
                HTG
            );

            var promoLine = result.ticketLines().get(1);
            assertThat(promoLine.selection().key().value()).isEqualTo("77");
            assertThat(promoLine.selectionSource()).isEqualTo(TicketLineSelectionSource.CUSTOMER_SELECTED);
        }
    }

    // -------------------------------------------------------------------------
    // No-op when NOT_ELIGIBLE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("NOT_ELIGIBLE decision")
    class NotEligible {

        @Test
        @DisplayName("returns original lines and charges unchanged")
        void noOpWhenNotEligible() {
            var notEligible = new PromotionDecision(
                PromotionDecisionId.of(UUID.randomUUID()),
                PromotionDecisionStatus.NOT_ELIGIBLE,
                PromotionEvaluationPhase.SALE_CONFIRMATION,
                NOW,
                "hash",
                "v1",
                List.of(),
                List.of()
            );
            var line = customerLine();
            var charge = new TicketCharge(TicketChargeType.BUYER_SMS, money("5"), ChargePaidBy.BUYER);

            var result = applier.apply(notEligible, List.of(line), List.of(charge), null, HTG);

            assertThat(result.ticketLines()).hasSize(1);
            assertThat(result.charges()).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static TicketLine customerLine() {
        return new TicketLine(
            TicketLineId.of(UUID.fromString("41000000-0000-0000-0000-000000000001")),
            1,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            new Selection(SelectionKey.of("05"), "05"),
            money("10"),
            money("10"),
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
            money("0")
        );
    }

    private static PromotionDecision decision(PromotionEffect effect) {
        return new PromotionDecision(
            PromotionDecisionId.of(UUID.fromString("D1000000-0000-0000-0000-000000000001")),
            PromotionDecisionStatus.APPLIED,
            PromotionEvaluationPhase.SALE_CONFIRMATION,
            NOW,
            "hash",
            "v1",
            List.of(effect),
            List.of()
        );
    }

    private static PromotionEffect effect(PromotionEffectType type, String gameCode, String appliesTo, BigDecimal amount) {
        return new PromotionEffect(
            PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001")),
            type,
            gameCode,
            0,
            amount,
            "HTG",
            appliesTo,
            null,
            PromotionChoiceMode.NONE
        );
    }

    private static SellTicketCommand command() {
        return command(List.of());
    }

    private static SellTicketCommand command(List<PromotionChoiceInput> promotionChoices) {
        return new SellTicketCommand(
            DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001")),
            DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001")),
            HTG,
            List.of(),
            null,
            promotionChoices
        );
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }
}
