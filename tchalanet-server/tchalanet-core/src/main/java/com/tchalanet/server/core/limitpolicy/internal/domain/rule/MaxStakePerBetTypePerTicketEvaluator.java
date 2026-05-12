package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class MaxStakePerBetTypePerTicketEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_STAKE_PER_BET_TYPE_PER_TICKET;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        var valueCents = LimitRuleParams.requiredLong(rule, "valueCents");
        var totals = new EnumMap<BetType, Long>(BetType.class);

        for (var line : ctx.lines()) {
            totals.merge(line.betType(), line.stakeCents(), Long::sum);
        }

        var breaches = new ArrayList<LimitBreach>();

        for (var entry : totals.entrySet()) {
            if (entry.getValue() > valueCents) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.max_stake_per_bet_type_per_ticket",
                    "limit.max_stake_per_bet_type_per_ticket",
                    valueCents,
                    null,
                    entry.getValue()));
            }
        }

        return breaches;
    }
}
