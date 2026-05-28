package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.math.RoundingMode;

@Component
public class PromotionOddsBoostApplier {

    public void apply(
        List<TicketLine> lines,
        PromotionEffect effect,
        PromotionDecision decision,
        CurrencyCode currency
    ) {
        if (effect.type() != PromotionEffectType.BOOST_ODDS) {
            return;
        }

        // V1 targets BOOST_ODDS by gameCode only; betType/betOption targeting is future scope.
        var targetGameCode = effect.gameCode();
        if (targetGameCode == null || targetGameCode.isBlank()) {
            throw new IllegalArgumentException("promotion.boost_odds_game_required");
        }
        if (effect.amount() == null || effect.amount().signum() <= 0) {
            throw new IllegalArgumentException("promotion.boost_odds_amount_required");
        }
        var boostedOdds = effect.amount().setScale(4, RoundingMode.UNNECESSARY);

        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);

            if (!line.gameCode().name().equals(targetGameCode)) {
                continue;
            }

            var potential = line.payoutBaseAmount()
                .amount()
                .multiply(boostedOdds)
                .setScale(2, RoundingMode.HALF_UP);

            lines.set(i, line.withPromotionPricing(
                boostedOdds,
                new Money(potential, currency),
                decision.decisionId(),
                promotionLabel(effect),
                effect.type().name()
            ));
        }
    }

    private String promotionLabel(PromotionEffect effect) {
        if (effect.reason() != null && !effect.reason().isBlank()) {
            return effect.reason().trim();
        }
        return TicketReceiptI18nKeys.PROMOTION_BOOST_ODDS;
    }
}
