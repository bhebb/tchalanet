package com.tchalanet.server.core.promotion.internal.application.service.lifecycle;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PromotionCampaignActivationPolicy")
class PromotionCampaignActivationPolicyTest {

    private static final Instant T0 = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-07-01T00:00:00Z");

    private final PromotionCampaignActivationPolicy policy = new PromotionCampaignActivationPolicy();

    @Nested
    @DisplayName("Date validation")
    class Dates {
        @Test
        @DisplayName("null startsAt is rejected")
        void nullStartsAt() {
            var campaign = campaign(null, T1, List.of(ruleWith(PromotionEffectType.WAIVE_CHARGE)));
            assertThatThrownBy(() -> policy.validate(campaign)).isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("null endsAt is rejected")
        void nullEndsAt() {
            var campaign = campaign(T0, null, List.of(ruleWith(PromotionEffectType.WAIVE_CHARGE)));
            assertThatThrownBy(() -> policy.validate(campaign)).isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("startsAt must be before endsAt")
        void invalidWindow() {
            var campaign = campaign(T1, T0, List.of(ruleWith(PromotionEffectType.WAIVE_CHARGE)));
            assertThatThrownBy(() -> policy.validate(campaign)).isInstanceOf(ProblemRestException.class);
        }
    }

    @Nested
    @DisplayName("Rule validation")
    class Rules {
        @Test
        @DisplayName("no rules is rejected")
        void noRules() {
            var campaign = campaign(T0, T1, List.of());
            assertThatThrownBy(() -> policy.validate(campaign)).isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("rule with no effects is rejected")
        void ruleWithNoEffects() {
            var campaign = campaign(T0, T1, List.of(rule(List.of())));
            assertThatThrownBy(() -> policy.validate(campaign)).isInstanceOf(ProblemRestException.class);
        }
    }

    @Nested
    @DisplayName("Valid campaigns")
    class Valid {
        @Test
        @DisplayName("WAIVE_CHARGE rule passes")
        void waiveCharge() {
            var campaign = campaign(T0, T1, List.of(ruleWith(PromotionEffectType.WAIVE_CHARGE)));
            assertThatCode(() -> policy.validate(campaign)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("BOOST_ODDS rule passes")
        void boostOdds() {
            var campaign = campaign(T0, T1, List.of(ruleWith(PromotionEffectType.BOOST_ODDS)));
            assertThatCode(() -> policy.validate(campaign)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("FREE_GAME_LINE rule passes")
        void freeGameLine() {
            var campaign = campaign(T0, T1, List.of(ruleWith(PromotionEffectType.FREE_GAME_LINE)));
            assertThatCode(() -> policy.validate(campaign)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("FREE_GAME_LINE with RANDOM generation strategy passes")
        void freeGameLineRandomStrategy() {
            var campaign = campaign(T0, T1, List.of(rule(List.of(new PromotionEffectConfigView(
                PromotionEffectType.FREE_GAME_LINE,
                Map.of("choiceMode", "AUTO_GENERATE", "generationStrategy", "RANDOM"))))));
            assertThatCode(() -> policy.validate(campaign)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Generation strategy validation")
    class GenerationStrategy {
        @Test
        @DisplayName("LOW_EXPOSURE_RANDOM is rejected at activation in V1")
        void lowExposureRandomRejected() {
            var campaign = campaign(T0, T1, List.of(rule(List.of(new PromotionEffectConfigView(
                PromotionEffectType.FREE_GAME_LINE,
                Map.of("choiceMode", "AUTO_GENERATE", "generationStrategy", "LOW_EXPOSURE_RANDOM"))))));
            assertThatThrownBy(() -> policy.validate(campaign)).isInstanceOf(ProblemRestException.class);
        }
    }

    private static PromotionCampaignView campaign(Instant startsAt, Instant endsAt, List<PromotionRuleView> rules) {
        return new PromotionCampaignView(null, "TEST", "Test Campaign", PromotionCampaignStatus.DRAFT, 100, startsAt, endsAt, rules);
    }

    private static PromotionRuleView ruleWith(PromotionEffectType effectType) {
        return rule(List.of(new PromotionEffectConfigView(effectType, Map.of())));
    }

    private static PromotionRuleView rule(List<PromotionEffectConfigView> effects) {
        return new PromotionRuleView(null, "rule-key", 100, List.of(), effects);
    }
}
