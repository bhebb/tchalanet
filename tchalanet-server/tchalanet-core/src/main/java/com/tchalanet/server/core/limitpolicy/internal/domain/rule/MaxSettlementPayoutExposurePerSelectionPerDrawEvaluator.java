package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MaxSettlementPayoutExposurePerSelectionPerDrawEvaluator implements LimitRuleEvaluator {

    @Override
    public RuleKey supports() {
        return RuleKey.MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW;
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
                line.settlementPayoutCents(),
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

            var next = current.settlementPayoutExposureTotalCents() + delta;

            if (next > valueCents) {
                breaches.add(new LimitBreach(
                    rule.ruleKey(),
                    rule.onBreach(),
                    rule.appliedScope(),
                    "limit.max_settlement_payout_exposure_per_selection_per_draw",
                    "limit.max_settlement_payout_exposure_per_selection_per_draw",
                    valueCents,
                    current.settlementPayoutExposureTotalCents(),
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
