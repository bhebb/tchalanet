package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import com.tchalanet.server.core.promotion.internal.domain.model.SellerTerminalPromotionEffectOverride;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromotionTerminalOverrideMerger")
class PromotionTerminalOverrideMergerTest {

    private final PromotionTerminalOverrideMerger merger = new PromotionTerminalOverrideMerger();

    @Test
    @DisplayName("overrides only the matching terminal effect")
    void overridesMatchingEffect() {
        var rule = maryajRule();
        var override = new SellerTerminalPromotionEffectOverride(
            UUID.fromString("E1000000-0000-0000-0000-000000000001"),
            SellerTerminalId.of(UUID.fromString("D1000000-0000-0000-0000-000000000001")),
            rule.campaignId(),
            rule.id(),
            PromotionEffectType.FREE_GAME_LINE,
            "HT_MARYAJ_GRATIS",
            true,
            2,
            PromotionChoiceMode.SELLER_SELECTS,
            null,
            false,
            0
        );

        var effective = merger.merge(List.of(rule), List.of(override));

        assertThat(effective.terminalOverrideApplied()).isTrue();
        assertThat(effective.overrideHash()).isNotBlank();
        var effect = effective.rules().getFirst().effects().getFirst();
        assertThat(effect.quantity()).isEqualTo(2);
        assertThat(effect.choiceMode()).isEqualTo(PromotionChoiceMode.SELLER_SELECTS);
        assertThat(effect.regenerableBeforeConfirm()).isFalse();
        assertThat(effect.maxRegenerationsBeforeConfirm()).isZero();
    }

    @Test
    @DisplayName("passes through all effects when no overrides match")
    void passesThrough() {
        var rule = maryajRule();
        var override = new SellerTerminalPromotionEffectOverride(
            UUID.fromString("E1000000-0000-0000-0000-000000000003"),
            SellerTerminalId.of(UUID.fromString("D1000000-0000-0000-0000-000000000001")),
            rule.campaignId(),
            rule.id(),
            PromotionEffectType.BOOST_ODDS,
            "HT_SOME_OTHER",
            true,
            5,
            null,
            null,
            null,
            null
        );

        var effective = merger.merge(List.of(rule), List.of(override));

        assertThat(effective.terminalOverrideApplied()).isTrue();
        assertThat(effective.rules().getFirst().effects()).hasSize(1);
        var effect = effective.rules().getFirst().effects().getFirst();
        assertThat(effect.quantity()).isEqualTo(rule.effects().getFirst().quantity());
    }

    @Test
    @DisplayName("returns rules unchanged when overrides list is null")
    void nullOverrides() {
        var rule = maryajRule();
        var effective = merger.merge(List.of(rule), null);

        assertThat(effective.terminalOverrideApplied()).isFalse();
        assertThat(effective.overrideHash()).isNull();
        assertThat(effective.rules()).hasSize(1);
    }

    @Test
    @DisplayName("returns rules unchanged when overrides list is empty")
    void emptyOverrides() {
        var rule = maryajRule();
        var effective = merger.merge(List.of(rule), List.of());

        assertThat(effective.terminalOverrideApplied()).isFalse();
        assertThat(effective.overrideHash()).isNull();
    }

    @Test
    @DisplayName("removes a disabled terminal effect")
    void disablesMatchingEffect() {
        var rule = maryajRule();
        var override = new SellerTerminalPromotionEffectOverride(
            UUID.fromString("E1000000-0000-0000-0000-000000000002"),
            SellerTerminalId.of(UUID.fromString("D1000000-0000-0000-0000-000000000001")),
            rule.campaignId(),
            rule.id(),
            PromotionEffectType.FREE_GAME_LINE,
            "HT_MARYAJ_GRATIS",
            false,
            null,
            null,
            null,
            null,
            null
        );

        var effective = merger.merge(List.of(rule), List.of(override));

        assertThat(effective.rules().getFirst().effects()).isEmpty();
    }

    private PromotionRule maryajRule() {
        var campaignId = PromotionCampaignId.of(UUID.fromString("A1000000-0000-0000-0000-000000000001"));
        var ruleId = PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001"));
        return new PromotionRule(
            ruleId,
            campaignId,
            "maryaj-gratis",
            100,
            null,
            null,
            List.of(),
            List.of(new PromotionEffect(
                ruleId,
                campaignId,
                "maryaj-gratis",
                PromotionEffectType.FREE_GAME_LINE,
                "HT_MARYAJ_GRATIS",
                1,
                BigDecimal.ONE,
                "HTG",
                null,
                null,
                PromotionChoiceMode.AUTO_GENERATE
            ).withTerminalOverride(null, null, SelectionGenerationStrategy.RANDOM, true, 3))
        );
    }
}
