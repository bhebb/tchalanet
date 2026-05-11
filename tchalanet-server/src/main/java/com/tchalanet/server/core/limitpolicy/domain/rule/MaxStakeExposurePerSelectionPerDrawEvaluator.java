package com.tchalanet.server.core.limitpolicy.domain.rule;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MaxStakeExposurePerSelectionPerDrawEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        var valueCents = LimitRuleParams.requiredLong(rule, "valueCents");

        var deltaBySelection = new HashMap<SelectionKey, Long>();

        for (var line : ctx.lines()) {
            deltaBySelection.merge(
                new SelectionKey(line.betType(), line.selectionKey()),
                line.stakeCents(),
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

            var next = current.stakeTotalCents() + delta;

            if (next > valueCents) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.max_stake_exposure_per_selection_per_draw",
                    "limit.max_stake_exposure_per_selection_per_draw",
                    valueCents,
                    current.stakeTotalCents(),
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
