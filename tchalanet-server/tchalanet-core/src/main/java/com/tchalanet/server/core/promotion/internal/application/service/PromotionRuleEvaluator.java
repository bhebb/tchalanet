package com.tchalanet.server.core.promotion.internal.application.service;

import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import java.math.RoundingMode;
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
            .map(effect -> applyQuantityMode(effect, ctx))
            .filter(effect -> effect.quantity() > 0)
            .toList();
    }

    private PromotionEffect applyQuantityMode(PromotionEffect effect, PromotionEvaluationContext ctx) {
        if (effect.quantityMode() == PromotionQuantityMode.PER_PAID_AMOUNT) {
            return applyPerPaidAmount(effect, ctx);
        }
        if (effect.quantityMode() == PromotionQuantityMode.TIERED_PAID_AMOUNT) {
            return applyTieredPaidAmount(effect, ctx);
        }
        return effect;
    }

    private PromotionEffect applyPerPaidAmount(PromotionEffect effect, PromotionEvaluationContext ctx) {
        var paidTotal = ctx.paidTotal();
        var step = effect.stepPaidAmount();
        if (paidTotal == null || step == null || paidTotal.signum() <= 0 || step.signum() <= 0) {
            return effect.withQuantity(0);
        }

        var steps = paidTotal.divide(step, 0, RoundingMode.DOWN).intValue();
        var quantity = steps * Math.max(effect.quantityPerStep(), 1);
        var capped = Math.min(quantity, Math.max(effect.maxQuantity(), 0));
        return effect.withQuantity(capped);
    }

    private PromotionEffect applyTieredPaidAmount(PromotionEffect effect, PromotionEvaluationContext ctx) {
        var paidTotal = ctx.paidTotal();
        if (paidTotal == null || paidTotal.signum() <= 0) {
            return effect.withQuantity(0);
        }

        return effect.quantityTiers().stream()
            .filter(tier -> paidTotal.compareTo(tier.minPaidAmount()) >= 0)
            .filter(tier -> tier.maxPaidAmount() == null || paidTotal.compareTo(tier.maxPaidAmount()) <= 0)
            .findFirst()
            .map(tier -> effect.withQuantity(tier.quantity()))
            .orElseGet(() -> effect.withQuantity(0));
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
