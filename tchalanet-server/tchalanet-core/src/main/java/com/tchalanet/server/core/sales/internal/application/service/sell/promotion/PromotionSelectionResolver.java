package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import org.springframework.stereotype.Component;

@Component
public class PromotionSelectionResolver {

    public record SelectionResult(String rawSelection, String source) {}

    public SelectionResult resolveSelection(
        PromotionDecision decision,
        PromotionEffect effect,
        SellTicketCommand command,
        int index,
        BetType betType,
        Short betOption
    ) {
        if (command != null && command.promotionChoices() != null) {
            var match = command.promotionChoices().stream()
                .filter(c -> c != null
                    && c.rawSelection() != null
                    && effect.gameCode() != null
                    && effect.gameCode().equals(c.gameCode())
                    && c.index() == index)
                .findFirst();
            if (match.isPresent()) {
                return new SelectionResult(match.get().rawSelection(), "CUSTOMER_SELECTED");
            }
        }
        return new SelectionResult("1", "PROMOTION");
    }
}
