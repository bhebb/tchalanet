package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

import java.util.List;

public final class MaxStakePerTicketEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_STAKE_PER_TICKET;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        var valueCents = LimitRuleParams.requiredLong(rule, "valueCents");
        var total = ctx.totalStakeCents();

        if (total <= valueCents) {
            return List.of();
        }

        return List.of(new LimitBreach(
            rule.ruleKey(),
            rule.onBreach(),
            rule.appliedScope(),
            "limit.max_stake_per_ticket",
            "limit.max_stake_per_ticket",
            valueCents,
            null,
            total));
    }
}
