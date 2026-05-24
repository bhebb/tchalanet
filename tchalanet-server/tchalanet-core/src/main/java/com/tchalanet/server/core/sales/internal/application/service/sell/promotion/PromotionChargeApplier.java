package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectType;
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

        var chargeType = effect.appliesTo(); // ex: BUYER_SMS

        charges.removeIf(c -> c.type().name().equals(chargeType));

        // Option alternative:
        // garder une charge amount=0 pour affichage/audit.
    }
}
