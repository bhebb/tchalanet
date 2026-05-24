package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class MaxStakePerLineEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_STAKE_PER_LINE;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        var valueCents = LimitRuleParams.requiredLong(rule, "valueCents");
        var breaches = new ArrayList<LimitBreach>();

        for (var line : ctx.lines()) {
            if (line.stakeCents() > valueCents) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.max_stake_per_line",
                    "limit.max_stake_per_line",
                    valueCents,
                    null,
                    line.stakeCents()));
            }
        }

        return breaches;
    }
}
