package com.tchalanet.server.core.limitpolicy.domain.rule;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

import java.util.List;

public final class MaxLinesPerTicketEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_LINES_PER_TICKET;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        var maxCount = LimitRuleParams.requiredLong(rule, "maxCount");
        var linesCount = ctx.linesCount();

        if (linesCount <= maxCount) {
            return List.of();
        }

        return List.of(new LimitBreach(
            rule.ruleKey(),
            rule.onBreach(),
            rule.appliedScope(),
            "limit.max_lines_per_ticket",
            "limit.max_lines_per_ticket",
            maxCount,
            null,
            (long) linesCount));
    }
}
