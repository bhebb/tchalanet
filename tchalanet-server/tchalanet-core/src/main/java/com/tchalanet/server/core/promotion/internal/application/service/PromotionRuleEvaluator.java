package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.core.promotion.api.model.*;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * MVP evaluator. Replace by a richer rule engine later without changing sales.
 * Supports examples: WAIVE_CHARGE, BOOST_ODDS, FREE_GAME_LINE, FREE_EXTRA_LINES.
 */
@Component
public class PromotionRuleEvaluator {
    public Optional<PromotionEffect> evaluate(PromotionRule rule, PromotionEvaluationContext ctx) {
        if (!eligible(rule, ctx)) {
            return Optional.empty();
        }
        var effect = rule.effect();
        var amount = effect.get("amount") == null ? null : new BigDecimal(String.valueOf(effect.get("amount")));
        int quantity = effect.get("quantity") == null ? 0 : Integer.parseInt(String.valueOf(effect.get("quantity")));
        var choiceMode = effect.get("choiceMode") == null
            ? PromotionChoiceMode.NONE
            : PromotionChoiceMode.valueOf(String.valueOf(effect.get("choiceMode")));
        return Optional.of(new PromotionEffect(
            rule.id(),
            rule.effectType(),
            str(effect.get("gameCode")),
            quantity,
            amount,
            str(effect.get("currency")),
            str(effect.get("appliesTo")),
            str(effect.get("reason")),
            choiceMode
        ));
    }

    private boolean eligible(PromotionRule rule, PromotionEvaluationContext ctx) {
        var e = rule.eligibility();
        if (e.get("minPaidTotal") != null) {
            var min = new BigDecimal(String.valueOf(e.get("minPaidTotal")));
            if (ctx.paidTotal() == null || ctx.paidTotal().compareTo(min) < 0) return false;
        }
        if (e.get("requiredGameCode") != null) {
            if (!ctx.paidGameCodes().contains(String.valueOf(e.get("requiredGameCode")))) return false;
        }
        // beforeTime/zone/quota handled by future specialized evaluators/ports.
        return true;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
