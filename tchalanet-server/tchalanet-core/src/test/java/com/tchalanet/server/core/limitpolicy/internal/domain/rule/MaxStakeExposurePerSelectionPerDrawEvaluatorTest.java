package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitLineContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MaxStakeExposurePerSelectionPerDrawEvaluatorTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final LimitScopeRef SCOPE =
        LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));
    private static final BetType BET = BetType.MATCH_1_2D;
    private static final String SELECTION = "05";

    private final MaxStakeExposurePerSelectionPerDrawEvaluator evaluator =
        new MaxStakeExposurePerSelectionPerDrawEvaluator();

    @Test
    void supports_the_stake_exposure_rule() {
        assertThat(evaluator.supports())
            .isEqualTo(RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW);
    }

    @Test
    void new_stake_that_stays_within_the_limit_does_not_breach() {
        // 800 already exposed + 100 new = 900 <= 1000
        var breaches = evaluate(1000, existingStake(800), line(100));
        assertThat(breaches).isEmpty();
    }

    @Test
    void new_stake_that_crosses_the_limit_breaches_with_current_and_delta_reported() {
        // 800 already exposed + 300 new = 1100 > 1000
        var breaches = evaluate(1000, existingStake(800), line(300));

        assertThat(breaches).hasSize(1);
        var breach = breaches.get(0);
        assertThat(breach.ruleKey()).isEqualTo(RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW);
        assertThat(breach.limitValue()).isEqualTo(1000L);
        assertThat(breach.currentValue()).isEqualTo(800L);
        assertThat(breach.deltaValue()).isEqualTo(300L);
    }

    @Test
    void stakes_on_the_same_selection_are_aggregated_before_comparison() {
        // 800 existing + (150 + 150) on the same selection = 1100 > 1000 → one breach
        var breaches = evaluate(1000, existingStake(800), line(150), line(150));

        assertThat(breaches).hasSize(1);
        assertThat(breaches.get(0).deltaValue()).isEqualTo(300L);
    }

    @Test
    void a_selection_with_no_prior_exposure_starts_from_zero() {
        // no facts → current 0, 400 new stays under 1000
        var breaches = evaluate(1000, new LimitFactsSnapshot(Map.of()), line(400));
        assertThat(breaches).isEmpty();
    }

    private List<LimitBreach> evaluate(long valueCents, LimitFactsSnapshot facts, LimitLineContext... lines) {
        var params = MAPPER.createObjectNode().put("valueCents", valueCents);
        var rule = new EffectiveLimitRule(
            RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW, BreachOutcome.BLOCK, SCOPE, null, params);
        return evaluator.evaluate(rule, facts, context(lines));
    }

    private static LimitFactsSnapshot existingStake(long stakeCents) {
        var key = new LimitFactsSnapshot.Key(SCOPE, BET, SELECTION);
        var fact = new LimitFactsSnapshot.Fact(stakeCents, 0L, 0L);
        return new LimitFactsSnapshot(Map.of(key, fact));
    }

    private static LimitLineContext line(long stakeCents) {
        return new LimitLineContext(BET, SELECTION, stakeCents, 0L);
    }

    private static LimitContext context(LimitLineContext... lines) {
        return new LimitContext(
            TenantId.of(UUID.randomUUID()), null, null, null, null, Instant.now(), List.of(lines));
    }
}
