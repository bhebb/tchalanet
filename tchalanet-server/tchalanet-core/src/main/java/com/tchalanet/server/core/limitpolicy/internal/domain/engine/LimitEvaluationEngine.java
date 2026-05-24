package com.tchalanet.server.core.limitpolicy.internal.domain.engine;

import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimits;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitEvaluationResult;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;
import com.tchalanet.server.core.limitpolicy.internal.domain.rule.LimitRuleEvaluator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LimitEvaluationEngine {

    private final Map<RuleKey, LimitRuleEvaluator> evaluators;

    public LimitEvaluationEngine(List<LimitRuleEvaluator> evaluators) {
        if (evaluators == null || evaluators.isEmpty()) {
            throw new IllegalArgumentException("At least one LimitRuleEvaluator is required");
        }

        var registry = new EnumMap<RuleKey, LimitRuleEvaluator>(RuleKey.class);

        for (var evaluator : evaluators) {
            if (evaluator == null) {
                throw new IllegalArgumentException("LimitRuleEvaluator must not be null");
            }

            var ruleKey = evaluator.supports();

            if (ruleKey == null) {
                throw new IllegalArgumentException(
                    evaluator.getClass().getSimpleName() + " returned null RuleKey");
            }

            var previous = registry.putIfAbsent(ruleKey, evaluator);

            if (previous != null) {
                throw new IllegalStateException(
                    "Duplicate evaluator for " + ruleKey
                        + ": "
                        + previous.getClass().getSimpleName()
                        + " and "
                        + evaluator.getClass().getSimpleName());
            }
        }

        this.evaluators = Map.copyOf(registry);
    }

    public LimitEvaluationResult evaluate(
        EffectiveLimits effectiveLimits,
        LimitFactsSnapshot facts,
        LimitContext context
    ) {
        if (effectiveLimits == null) {
            throw new IllegalArgumentException("effectiveLimits is required");
        }

        if (facts == null) {
            throw new IllegalArgumentException("facts is required");
        }

        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }

        var breaches = new ArrayList<LimitBreach>();

        for (EffectiveLimitRule rule : effectiveLimits.rules().values()) {
            var evaluator = evaluators.get(rule.ruleKey());

            if (evaluator == null) {
                throw new IllegalStateException("No LimitRuleEvaluator registered for " + rule.ruleKey());
            }

            var ruleBreaches = evaluator.evaluate(rule, facts, context);

            if (ruleBreaches != null && !ruleBreaches.isEmpty()) {
                breaches.addAll(ruleBreaches);
            }
        }

        return LimitEvaluationResult.from(breaches);
    }
}
