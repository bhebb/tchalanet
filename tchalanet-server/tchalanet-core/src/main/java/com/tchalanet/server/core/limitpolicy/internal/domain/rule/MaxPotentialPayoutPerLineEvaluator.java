package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class MaxPotentialPayoutPerLineEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_POTENTIAL_PAYOUT_PER_LINE;
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
            if (line.potentialPayoutCents() > valueCents) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.max_potential_payout_per_line",
                    "limit.max_potential_payout_per_line",
                    valueCents,
                    null,
                    line.potentialPayoutCents()));
            }
        }

        return breaches;
    }
}
