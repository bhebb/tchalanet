package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MaxSalesCountPerSelectionPerDrawEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_SALES_COUNT_PER_SELECTION_PER_DRAW;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        var maxCount = LimitRuleParams.requiredLong(rule, "maxCount");

        var deltaBySelection = new HashMap<SelectionKey, Long>();

        for (var line : ctx.lines()) {
            deltaBySelection.merge(
                new SelectionKey(line.betType(), line.selectionKey()),
                1L,
                Long::sum);
        }

        var breaches = new ArrayList<LimitBreach>();

        for (var entry : deltaBySelection.entrySet()) {
            var selection = entry.getKey();
            var delta = entry.getValue();

            var current = facts.fact(
                rule.appliedScope(),
                selection.betType(),
                selection.selectionKey());

            var next = current.salesCount() + delta;

            if (next > maxCount) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.max_sales_count_per_selection_per_draw",
                    "limit.max_sales_count_per_selection_per_draw",
                    maxCount,
                    current.salesCount(),
                    delta));
            }
        }

        return breaches;
    }

    private record SelectionKey(
        BetType betType,
        String selectionKey
    ) {}
}
