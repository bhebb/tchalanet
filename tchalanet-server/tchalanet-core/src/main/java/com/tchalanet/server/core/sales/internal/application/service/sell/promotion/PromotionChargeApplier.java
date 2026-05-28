package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromotionChargeApplier {

    public void apply(
        List<TicketCharge> charges,
        PromotionEffect effect,
        PromotionDecision decision
    ) {
        if (effect.type() != PromotionEffectType.WAIVE_CHARGE) {
            return;
        }

        var chargeType = effect.appliesTo();

        // Mark as waived (keeps original amount for print/audit) instead of removing.
        // isBuyerFacing() returns false for waived charges so the total is unaffected.
        charges.replaceAll(c -> c.type().name().equals(chargeType)
            ? c.asWaived(
                decision.decisionId(),
                effect.ruleId(),
                effect.type().name(),
                promotionLabel(effect))
            : c);
    }

    private String promotionLabel(PromotionEffect effect) {
        if (effect.reason() != null && !effect.reason().isBlank()) {
            return effect.reason().trim();
        }
        return effect.type().name();
    }
}
