package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class BlockBetTypeEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.BLOCK_BET_TYPE;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        var betType = BetType.valueOf(LimitRuleParams.requiredText(rule, "betType"));
        var breaches = new ArrayList<LimitBreach>();

        for (var line : ctx.lines()) {
            if (line.betType() == betType) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.block_bet_type",
                    "limit.block_bet_type",
                    null,
                    null,
                    line.stakeCents()));
            }
        }

        return breaches;
    }
}
