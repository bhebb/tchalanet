package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.selection.SelectionGenerationPurpose;
import com.tchalanet.server.core.sales.api.model.selection.SelectionGenerationStrategy;
import com.tchalanet.server.core.sales.internal.application.service.sell.generation.SelectionGenerationService;
import org.springframework.stereotype.Component;

@Component
public class PromotionSelectionResolver {

    private final SelectionGenerationService selectionGenerationService;

    public PromotionSelectionResolver(SelectionGenerationService selectionGenerationService) {
        this.selectionGenerationService = selectionGenerationService;
    }

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

        var generated = selectionGenerationService.generate(
            GameCode.valueOf(effect.gameCode()),
            betType,
            betOption,
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.PROMOTION_FREE_LINE
        );
        return new SelectionResult(generated.key().value(), TicketLineSelectionSource.PROMOTION_GENERATED);
    }
}
