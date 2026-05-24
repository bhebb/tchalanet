package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.promotion.api.model.PromotionEffect;
import com.tchalanet.server.catalog.game.api.model.BetType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PromotionalPricingResolver {
    public PromotionalPricing resolve(PromotionEffect effect, CurrencyCode currency) {
        // MVP: effect.amount is payoutBaseAmount, odds comes from effect or pricing catalog later.
        // Replace by catalog.pricing lookup for MARYAJ_GRATUIT/BOULE_GRATUIT.
        return new PromotionalPricing(BetType.MARYAJ, BigDecimal.TEN);
    }

    public record PromotionalPricing(BetType betType, BigDecimal odds) {}
}
