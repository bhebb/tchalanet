package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectType;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Applies PromotionDecision effects to sale materialization.
 * V1 creates/updates TicketLine snapshots. Settlement must later consume only these snapshots.
 */
@Component
@RequiredArgsConstructor
public class SalePromotionEffectApplier {
    private final PromotionalTicketLineFactory promotionalTicketLineFactory;

    public List<TicketLine> applyToLines(
        List<TicketLine> paidLines,
        PromotionDecision decision,
        CurrencyCode currency
    ) {
        if (decision == null || !decision.applied()) {
            return List.copyOf(paidLines);
        }
        var out = new ArrayList<TicketLine>(paidLines);
        for (var effect : decision.effects()) {
            applyEffect(out, decision, effect, currency);
        }
        return List.copyOf(out);
    }

    private void applyEffect(
        List<TicketLine> lines,
        PromotionDecision decision,
        PromotionEffect effect,
        CurrencyCode currency
    ) {
        if (effect.type() == PromotionEffectType.FREE_GAME_LINE || effect.type() == PromotionEffectType.FREE_EXTRA_LINES) {
            for (int i = 0; i < effect.quantity(); i++) {
                lines.add(promotionalTicketLineFactory.create(decision, effect, nextLineNumber(lines), currency));
            }
        } else if (effect.type() == PromotionEffectType.BOOST_ODDS) {
            // Keep simple: factory/mutator should return new TicketLine with boosted odds snapshot.
            // Implement in domain once TicketLine gains copyWithPricingSnapshot.
        }
    }

    private int nextLineNumber(List<TicketLine> lines) {
        return lines.stream().mapToInt(TicketLine::lineNumber).max().orElse(0) + 1;
    }
}
