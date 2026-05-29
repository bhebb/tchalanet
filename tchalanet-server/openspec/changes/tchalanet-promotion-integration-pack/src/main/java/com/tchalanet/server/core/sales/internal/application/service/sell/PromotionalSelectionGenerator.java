package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.core.promotion.api.model.PromotionEffect;
import org.springframework.stereotype.Component;

@Component
public class PromotionalSelectionGenerator {
    public String generate(PromotionEffect effect) {
        // MVP placeholder. For CUSTOMER_SELECTS, command input must supply choices.
        // For AUTO_GENERATE, integrate random/secure generator per game.
        return "00-00";
    }
}
