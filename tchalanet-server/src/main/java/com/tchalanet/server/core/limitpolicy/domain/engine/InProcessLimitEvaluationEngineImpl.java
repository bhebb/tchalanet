package com.tchalanet.server.core.limitpolicy.domain.engine;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.application.query.model.LimitBreachView;
import com.tchalanet.server.core.limitpolicy.application.query.model.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimits;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InProcessLimitEvaluationEngineImpl implements InProcessLimitEvaluationEngine {


    @Override
    public LimitEvaluationView evaluate(EffectiveLimits limits, LimitFactsSnapshot facts, LimitContext ctx) {
        List<LimitBreachView> breaches = new ArrayList<>();
        var targetKey = ctx.scope().key();

        // MAX_STAKE_PER_TICKET (stateless)
        var maxTicket = limits.get(RuleKey.MAX_STAKE_PER_TICKET);
        if (maxTicket != null && ctx.ticketStakeTotal() != null && ctx.ticketStakeTotal().compareTo(maxTicket) > 0) {
            breaches.add(breach(RuleKey.MAX_STAKE_PER_TICKET, BreachOutcome.BLOCK, targetKey,
                "limit.max_stake_per_ticket", "limit.max_stake_per_ticket",
                maxTicket, null, ctx.ticketStakeTotal()));
        }

        // MAX_LINES_PER_TICKET (stateless)
        var maxLines = limits.get(RuleKey.MAX_LINES_PER_TICKET);
        if (maxLines != null && ctx.linesCount() > maxLines.intValue()) {
            breaches.add(breach(RuleKey.MAX_LINES_PER_TICKET, BreachOutcome.BLOCK, targetKey,
                "limit.max_lines_per_ticket", "limit.max_lines_per_ticket",
                maxLines, null, BigDecimal.valueOf(ctx.linesCount())));
        }

        // MAX_EXPOSURE_PER_SELECTION_PER_DRAW (stateful stake)
        var maxStakeExposure = limits.get(RuleKey.MAX_EXPOSURE_PER_SELECTION_PER_DRAW);
        if (maxStakeExposure != null) {
            var deltaStake = groupDeltaStake(ctx);
            for (var e : deltaStake.entrySet()) {
                var current = facts.bySelection().getOrDefault(
                    e.getKey(),
                    new LimitFactsSnapshot.Fact(BigDecimal.ZERO, BigDecimal.ZERO, 0L)
                );
                var next = current.stakeTotal().add(e.getValue());
                if (next.compareTo(maxStakeExposure) > 0) {
                    breaches.add(breach(RuleKey.MAX_EXPOSURE_PER_SELECTION_PER_DRAW, BreachOutcome.BLOCK, targetKey,
                        "limit.max_exposure_per_selection_per_draw", "limit.max_exposure_per_selection_per_draw",
                        maxStakeExposure, current.stakeTotal(), e.getValue()));
                }
            }
        }

        // MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW (stateful payout)
        var maxPayoutExposure = limits.get(RuleKey.MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW);
        if (maxPayoutExposure != null) {
            var deltaPayout = groupDeltaPayout(ctx);
            for (var e : deltaPayout.entrySet()) {
                var current = facts.bySelection().getOrDefault(
                    e.getKey(),
                    new LimitFactsSnapshot.Fact(BigDecimal.ZERO, BigDecimal.ZERO, 0L)
                );
                var next = current.potentialPayoutTotal().add(e.getValue());
                if (next.compareTo(maxPayoutExposure) > 0) {
                    breaches.add(breach(RuleKey.MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW, BreachOutcome.BLOCK, targetKey,
                        "limit.max_potential_payout_exposure_per_selection_per_draw",
                        "limit.max_potential_payout_exposure_per_selection_per_draw",
                        maxPayoutExposure, current.potentialPayoutTotal(), e.getValue()));
                }
            }
        }

        return new LimitEvaluationView(summarize(breaches), breaches);
    }

    private static Map<LimitFactsSnapshot.Key, BigDecimal> groupDeltaStake(LimitContext ctx) {
        Map<LimitFactsSnapshot.Key, BigDecimal> map = new HashMap<>();
        for (var line : ctx.lines()) {
            var key = new LimitFactsSnapshot.Key(line.betType(), line.selectionKey());
            map.merge(key, line.stake(), BigDecimal::add);
        }
        return map;
    }

    private static Map<LimitFactsSnapshot.Key, BigDecimal> groupDeltaPayout(LimitContext ctx) {
        Map<LimitFactsSnapshot.Key, BigDecimal> map = new HashMap<>();
        for (var line : ctx.lines()) {
            var key = new LimitFactsSnapshot.Key(line.betType(), line.selectionKey());
            map.merge(key, line.potentialPayout(), BigDecimal::add);
        }
        return map;
    }

    private static LimitBreachView breach(
        RuleKey ruleKey, BreachOutcome outcome, String appliedTarget,
        String code, String messageKey,
        BigDecimal limitValue, BigDecimal currentValue, BigDecimal deltaValue
    ) {
        return new LimitBreachView(ruleKey, outcome, appliedTarget, code, messageKey, limitValue, currentValue, deltaValue);
    }

    private static BreachOutcome summarize(List<LimitBreachView> breaches) {
        if (breaches.stream().anyMatch(b -> b.outcome() == BreachOutcome.BLOCK)) return BreachOutcome.BLOCK;
        if (breaches.stream().anyMatch(b -> b.outcome() == BreachOutcome.WARN)) return BreachOutcome.WARN;
        return BreachOutcome.ALLOW;
    }
}
