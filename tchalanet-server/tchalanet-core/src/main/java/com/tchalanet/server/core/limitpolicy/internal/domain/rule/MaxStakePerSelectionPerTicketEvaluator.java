package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MaxStakePerSelectionPerTicketEvaluator implements LimitRuleEvaluator {

    private record SelectionKey(BetType betType, String selectionKey) {}

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_STAKE_PER_SELECTION_PER_TICKET;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext ctx
    ) {
        var valueCents = LimitRuleParams.requiredLong(rule, "valueCents");
        var totals = new HashMap<SelectionKey, Long>();

        for (var line : ctx.lines()) {
            totals.merge(
                new SelectionKey(line.betType(), line.selectionKey()),
                line.stakeCents(),
                Long::sum);
        }

        var breaches = new ArrayList<LimitBreach>();

        for (var entry : totals.entrySet()) {
            if (entry.getValue() > valueCents) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.max_stake_per_selection_per_ticket",
                    "limit.max_stake_per_selection_per_ticket",
                    valueCents,
                    null,
                    entry.getValue()));
            }
        }

        return breaches;
    }
}
