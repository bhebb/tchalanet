package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromotionRuleEvaluator")
class PromotionRuleEvaluatorTest {

    private final PromotionRuleEvaluator evaluator = new PromotionRuleEvaluator();

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
}
