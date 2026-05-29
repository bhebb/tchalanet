package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * V1 evaluator for the deliberately small promotion surface.
 */
@Component
public class PromotionRuleEvaluator {
    public List<PromotionEffect> evaluate(PromotionRule rule, PromotionEvaluationContext ctx) {
        if (!eligible(rule, ctx)) {
            return List.of();
        }
        return rule.effects().stream()
            .map(effect -> effect.withRuleContext(rule.campaignId(), rule.ruleKey()))
            .toList();
    }

    private boolean eligible(PromotionRule rule, PromotionEvaluationContext ctx) {
        if (rule.minPaidTotal() != null
            && (ctx.paidTotal() == null || ctx.paidTotal().compareTo(rule.minPaidTotal()) < 0)) {
            return false;
        }

        for (var line : rule.eligibilityLines()) {
            var count = ctx.paidGameCodes().stream()
                .filter(gameCode -> gameCode.equals(line.gameCode()))
                .count();
            if (count < line.minCount()) {
                return false;
            }
        }

        // beforeLocalTime requires tenant-zone resolution in the sale context.
        // The column is persisted now, but runtime enforcement waits for that context field.
        return true;
    }
}
