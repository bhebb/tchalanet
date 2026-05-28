package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import org.springframework.stereotype.Component;

@Component
public class PromotionSelectionResolver {

    public record SelectionResult(String rawSelection, TicketLineSelectionSource source) {}

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
                    && (c.decisionId() == null || c.decisionId().equals(decision.decisionId()))
                    && c.index() == index)
                .findFirst();
            if (match.isPresent()) {
                return new SelectionResult(
                    match.get().rawSelection(),
                    match.get().selectionSource() == null
                        ? TicketLineSelectionSource.CUSTOMER_SELECTED
                        : match.get().selectionSource()
                );
            }
        }

        if (effect.choiceMode() == PromotionChoiceMode.CUSTOMER_SELECTS
            || effect.choiceMode() == PromotionChoiceMode.SELLER_SELECTS) {
            throw new IllegalArgumentException("promotion.free_game_selection_required");
        }

        var generatedSelection = generatedSelectionFor(betType, betOption, decision, effect, index);
        return new SelectionResult(generatedSelection, TicketLineSelectionSource.PROMOTION_GENERATED);
    }

    private String generatedSelectionFor(
        BetType betType,
        Short betOption,
        PromotionDecision decision,
        PromotionEffect effect,
        int index
    ) {
        var seed = Math.abs(java.util.Objects.hash(
            decision.decisionId(),
            effect.ruleId(),
            effect.gameCode(),
            betType,
            betOption,
            index
        ));
        return String.format("%02d", seed % 100);
    }
}
