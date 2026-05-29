package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;

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
