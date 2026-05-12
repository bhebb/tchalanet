package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class BlockSelectionPerDrawEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.BLOCK_SELECTION_PER_DRAW;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        BetType betType = null;

        if (LimitRuleParams.hasText(rule, "betType")) {
            betType = BetType.valueOf(LimitRuleParams.requiredText(rule, "betType"));
        }

        var breaches = new ArrayList<LimitBreach>();

        for (var line : ctx.lines()) {
            if (betType != null && line.betType() != betType) {
                continue;
            }

            if (LimitRuleParams.stringArrayContains(rule, "selections", line.selectionKey())) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.block_selection_per_draw",
                    "limit.block_selection_per_draw",
                    null,
                    null,
                    line.stakeCents()));
            }
        }

        return breaches;
    }
}
