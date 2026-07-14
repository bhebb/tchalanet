package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityTier;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromotionRuleEvaluator")
class PromotionRuleEvaluatorTest {

    private final PromotionRuleEvaluator evaluator = new PromotionRuleEvaluator();

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");

    @Test
    @DisplayName("copies campaign and rule identity onto emitted effects")
    void copiesRuleContextToEffects() {
        var campaignId = PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000001"));
        var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001"));
        var rule = new PromotionRule(
            ruleId,
            campaignId,
            "maryaj-free-line",
            100,
            null,
            null,
            List.of(),
            List.of(new PromotionEffect(
                ruleId,
                PromotionEffectType.FREE_GAME_LINE,
                "HT_BOLET",
                1,
                new BigDecimal("125"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.NONE
            ))
        );

        var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
            null,
            PromotionEvaluationPhase.SALE_CONFIRMATION,
            Instant.parse("2026-05-27T00:00:00Z"),
            null,
            List.of(),
            null,
            List.of(),
            null,
            null,
            new BigDecimal("500"),
            "HTG",
            List.of("HT_BOLET"),
            false
        ));

        assertThat(effects).hasSize(1);
        assertThat(effects.getFirst().campaignId()).isEqualTo(campaignId);
        assertThat(effects.getFirst().ruleId()).isEqualTo(ruleId);
        assertThat(effects.getFirst().ruleKey()).isEqualTo("maryaj-free-line");
    }

    @Test
    @DisplayName("calculates free line quantity from paid amount steps")
    void calculatesQuantityPerPaidAmountStep() {
        var campaignId = PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000002"));
        var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000002"));
        var rule = new PromotionRule(
            ruleId,
            campaignId,
            "maryaj-per-amount",
            100,
            null,
            null,
            List.of(),
            List.of(new PromotionEffect(
                ruleId,
                null,
                null,
                PromotionEffectType.FREE_GAME_LINE,
                "HT_MARYAJ_GRATIS",
                1,
                PromotionQuantityMode.PER_PAID_AMOUNT,
                new BigDecimal("1000"),
                2,
                10,
                List.of(),
                new BigDecimal("50"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.AUTO_GENERATE,
                null,
                true,
                3
            ))
        );

        var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
            null,
            PromotionEvaluationPhase.SALE_CONFIRMATION,
            Instant.parse("2026-05-27T00:00:00Z"),
            null,
            List.of(),
            null,
            List.of(),
            null,
            null,
            new BigDecimal("6500"),
            "HTG",
            List.of("HT_BOLET"),
            false
        ));

        assertThat(effects).hasSize(1);
        assertThat(effects.getFirst().quantity()).isEqualTo(10);
        assertThat(effects.getFirst().quantityMode()).isEqualTo(PromotionQuantityMode.PER_PAID_AMOUNT);
    }

    @Test
    @DisplayName("does not emit an effect below the first paid amount step")
    void skipsPerPaidAmountEffectBelowFirstStep() {
        var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000003"));
        var rule = new PromotionRule(
            ruleId,
            PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000003")),
            "maryaj-per-amount",
            100,
            null,
            null,
            List.of(),
            List.of(new PromotionEffect(
                ruleId,
                null,
                null,
                PromotionEffectType.FREE_GAME_LINE,
                "HT_MARYAJ_GRATIS",
                1,
                PromotionQuantityMode.PER_PAID_AMOUNT,
                new BigDecimal("1000"),
                2,
                10,
                List.of(),
                new BigDecimal("50"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.AUTO_GENERATE,
                null,
                true,
                3
            ))
        );

        var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
            null,
            PromotionEvaluationPhase.SALE_CONFIRMATION,
            Instant.parse("2026-05-27T00:00:00Z"),
            null,
            List.of(),
            null,
            List.of(),
            null,
            null,
            new BigDecimal("999"),
            "HTG",
            List.of("HT_BOLET"),
            false
        ));

        assertThat(effects).isEmpty();
    }

    @Test
    @DisplayName("counts repeated paid game codes for line count eligibility")
    void countsRepeatedPaidGameCodes() {
        var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000005"));
        var rule = new PromotionRule(
            ruleId,
            PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000005")),
            "maryaj-three-lines",
            100,
            null,
            null,
            List.of(new com.tchalanet.server.core.promotion.internal.domain.model.PromotionRuleEligibilityLine(
                "HT_BOLET",
                3
            )),
            List.of(new PromotionEffect(
                ruleId,
                PromotionEffectType.FREE_GAME_LINE,
                "HT_MARYAJ_GRATIS",
                1,
                new BigDecimal("50"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.AUTO_GENERATE
            ))
        );

        var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
            null,
            PromotionEvaluationPhase.SALE_CONFIRMATION,
            Instant.parse("2026-05-27T00:00:00Z"),
            null,
            List.of(),
            null,
            List.of(),
            null,
            null,
            new BigDecimal("500"),
            "HTG",
            List.of("HT_BOLET", "HT_BOLET", "HT_BOLET"),
            false
        ));

        assertThat(effects).hasSize(1);
    }

    @Test
    @DisplayName("selects Maryaj gratis quantity from paid amount tiers")
    void calculatesQuantityFromPaidAmountTiers() {
        var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000004"));
        var rule = new PromotionRule(
            ruleId,
            PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000004")),
            "maryaj-tiered-amount",
            100,
            null,
            null,
            List.of(),
            List.of(new PromotionEffect(
                ruleId,
                null,
                null,
                PromotionEffectType.FREE_GAME_LINE,
                "HT_MARYAJ_GRATIS",
                1,
                PromotionQuantityMode.TIERED_PAID_AMOUNT,
                null,
                1,
                3,
                List.of(
                    new PromotionQuantityTier(new BigDecimal("100"), new BigDecimal("199"), 1),
                    new PromotionQuantityTier(new BigDecimal("200"), new BigDecimal("499"), 2),
                    new PromotionQuantityTier(new BigDecimal("500"), null, 3)
                ),
                new BigDecimal("50"),
                "HTG",
                null,
                null,
                PromotionChoiceMode.AUTO_GENERATE,
                null,
                true,
                3
            ))
        );

        var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
            null,
            PromotionEvaluationPhase.SALE_CONFIRMATION,
            Instant.parse("2026-05-27T00:00:00Z"),
            null,
            List.of(),
            null,
            List.of(),
            null,
            null,
            new BigDecimal("500"),
            "HTG",
            List.of("HT_BOLET"),
            false
        ));

        assertThat(effects).hasSize(1);
        assertThat(effects.getFirst().quantity()).isEqualTo(3);
        assertThat(effects.getFirst().quantityMode()).isEqualTo(PromotionQuantityMode.TIERED_PAID_AMOUNT);
    }

    @Nested
    @DisplayName("Eligibility")
    class Eligibility {

        @Test
        @DisplayName("rejects rule when paidTotal is below minPaidTotal")
        void rejectsBelowMinPaidTotal() {
            var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000010"));
            var rule = new PromotionRule(
                ruleId,
                PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000010")),
                "min-paid",
                100,
                new BigDecimal("500"),
                null,
                List.of(),
                List.of(new PromotionEffect(
                    ruleId, PromotionEffectType.FREE_GAME_LINE, "HT_BOLET", 1,
                    new BigDecimal("50"), "HTG", null, null, PromotionChoiceMode.NONE
                ))
            );

            var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
                null, PromotionEvaluationPhase.SALE_CONFIRMATION, NOW, null,
                List.of(), null, List.of(), null, null,
                new BigDecimal("499"), "HTG", List.of("HT_BOLET"), false
            ));

            assertThat(effects).isEmpty();
        }

        @Test
        @DisplayName("rejects rule when paidTotal is null and minPaidTotal is set")
        void rejectsNullPaidTotalWhenMinSet() {
            var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000011"));
            var rule = new PromotionRule(
                ruleId,
                PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000011")),
                "min-paid",
                100,
                new BigDecimal("100"),
                null,
                List.of(),
                List.of(new PromotionEffect(
                    ruleId, PromotionEffectType.FREE_GAME_LINE, "HT_BOLET", 1,
                    new BigDecimal("50"), "HTG", null, null, PromotionChoiceMode.NONE
                ))
            );

            var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
                null, PromotionEvaluationPhase.SALE_CONFIRMATION, NOW, null,
                List.of(), null, List.of(), null, null,
                null, "HTG", List.of("HT_BOLET"), false
            ));

            assertThat(effects).isEmpty();
        }

        @Test
        @DisplayName("rejects rule when game code count is insufficient")
        void rejectsInsufficientGameCodeCount() {
            var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000012"));
            var rule = new PromotionRule(
                ruleId,
                PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000012")),
                "needs-3-bolet",
                100,
                null,
                null,
                List.of(new com.tchalanet.server.core.promotion.internal.domain.model.PromotionRuleEligibilityLine(
                    "HT_BOLET", 3
                )),
                List.of(new PromotionEffect(
                    ruleId, PromotionEffectType.FREE_GAME_LINE, "HT_MARYAJ_GRATIS", 1,
                    new BigDecimal("50"), "HTG", null, null, PromotionChoiceMode.NONE
                ))
            );

            var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
                null, PromotionEvaluationPhase.SALE_CONFIRMATION, NOW, null,
                List.of(), null, List.of(), null, null,
                new BigDecimal("500"), "HTG", List.of("HT_BOLET", "HT_BOLET"), false
            ));

            assertThat(effects).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tiered edge cases")
    class TieredEdgeCases {

        @Test
        @DisplayName("returns zero quantity when paidTotal matches no tier")
        void noMatchingTier() {
            var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000013"));
            var rule = new PromotionRule(
                ruleId,
                PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000013")),
                "tiered",
                100,
                null,
                null,
                List.of(),
                List.of(new PromotionEffect(
                    ruleId, null, null,
                    PromotionEffectType.FREE_GAME_LINE, "HT_MARYAJ_GRATIS", 1,
                    PromotionQuantityMode.TIERED_PAID_AMOUNT, null, 1, 3,
                    List.of(new PromotionQuantityTier(new BigDecimal("200"), new BigDecimal("499"), 2)),
                    new BigDecimal("50"), "HTG", null, null,
                    PromotionChoiceMode.AUTO_GENERATE, null, true, 3
                ))
            );

            var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
                null, PromotionEvaluationPhase.SALE_CONFIRMATION, NOW, null,
                List.of(), null, List.of(), null, null,
                new BigDecimal("100"), "HTG", List.of("HT_BOLET"), false
            ));

            assertThat(effects).isEmpty();
        }

        @Test
        @DisplayName("returns zero quantity when paidTotal is null for tiered mode")
        void nullPaidTotalTiered() {
            var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000014"));
            var rule = new PromotionRule(
                ruleId,
                PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000014")),
                "tiered",
                100,
                null,
                null,
                List.of(),
                List.of(new PromotionEffect(
                    ruleId, null, null,
                    PromotionEffectType.FREE_GAME_LINE, "HT_MARYAJ_GRATIS", 1,
                    PromotionQuantityMode.TIERED_PAID_AMOUNT, null, 1, 3,
                    List.of(new PromotionQuantityTier(new BigDecimal("100"), null, 1)),
                    new BigDecimal("50"), "HTG", null, null,
                    PromotionChoiceMode.AUTO_GENERATE, null, true, 3
                ))
            );

            var effects = evaluator.evaluate(rule, new PromotionEvaluationContext(
                null, PromotionEvaluationPhase.SALE_CONFIRMATION, NOW, null,
                List.of(), null, List.of(), null, null,
                null, "HTG", List.of("HT_BOLET"), false
            ));

            assertThat(effects).isEmpty();
        }
    }
}
