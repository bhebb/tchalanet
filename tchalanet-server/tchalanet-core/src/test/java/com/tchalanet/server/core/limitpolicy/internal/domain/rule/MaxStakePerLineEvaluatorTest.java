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

class MaxStakePerLineEvaluatorTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final LimitScopeRef SCOPE =
        LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));

    private final MaxStakePerLineEvaluator evaluator = new MaxStakePerLineEvaluator();

    @Test
    void supports_the_max_stake_per_line_rule() {
        assertThat(evaluator.supports()).isEqualTo(RuleKey.MAX_STAKE_PER_LINE);
    }

    @Test
    void a_line_below_the_limit_does_not_breach() {
        var breaches = evaluate(500, line(400));
        assertThat(breaches).isEmpty();
    }

    @Test
    void a_line_exactly_at_the_limit_does_not_breach() {
        // boundary is exclusive: stake must exceed the limit to breach
        var breaches = evaluate(500, line(500));
        assertThat(breaches).isEmpty();
    }

    @Test
    void a_line_above_the_limit_breaches_with_the_stake_reported() {
        var breaches = evaluate(500, line(600));

        assertThat(breaches).hasSize(1);
        var breach = breaches.get(0);
        assertThat(breach.ruleKey()).isEqualTo(RuleKey.MAX_STAKE_PER_LINE);
        assertThat(breach.outcome()).isEqualTo(BreachOutcome.BLOCK);
        assertThat(breach.limitValue()).isEqualTo(500L);
        assertThat(breach.deltaValue()).isEqualTo(600L);
    }

    @Test
    void only_the_offending_line_breaches() {
        var breaches = evaluate(500, line(300), line(700));

        assertThat(breaches).hasSize(1);
        assertThat(breaches.get(0).deltaValue()).isEqualTo(700L);
    }

    private List<LimitBreach> evaluate(long valueCents, LimitLineContext... lines) {
        var params = MAPPER.createObjectNode().put("valueCents", valueCents);
        var rule = new EffectiveLimitRule(
            RuleKey.MAX_STAKE_PER_LINE, BreachOutcome.BLOCK, SCOPE, null, params);
        return evaluator.evaluate(rule, new LimitFactsSnapshot(Map.of()), context(lines));
    }

    private static LimitLineContext line(long stakeCents) {
        return new LimitLineContext(BetType.MATCH_1_2D, "05", stakeCents, 0L);
    }

    private static LimitContext context(LimitLineContext... lines) {
        return new LimitContext(
            TenantId.of(UUID.randomUUID()), null, null, null, null, Instant.now(), List.of(lines));
    }
}
