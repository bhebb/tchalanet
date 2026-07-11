package com.tchalanet.server.core.sales.internal.domain.model.preparation;

import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;

import java.util.UUID;

public record SalePreparationPromotionLine(
    String lineRef,
    String gameCode,
    String betType,
    Short betOption,
    String selection,
    TicketLineSelectionSource selectionSource,
    PromotionChoiceMode choiceMode,
    UUID promotionDecisionId,
    UUID promotionRuleId,
    String promotionRuleKey,
    String promotionEffectType,
    String promotionDecisionContextHash,
    String promotionDecisionEngineVersion,
    boolean regenerable,
    int maxRegenerations,
    int regenerationCount
) {
    public SalePreparationPromotionLine withSelection(String newSelection) {
        return new SalePreparationPromotionLine(
            lineRef, gameCode, betType, betOption, newSelection,
            TicketLineSelectionSource.PROMOTION_GENERATED, choiceMode,
            promotionDecisionId, promotionRuleId, promotionRuleKey, promotionEffectType,
            promotionDecisionContextHash, promotionDecisionEngineVersion, regenerable, maxRegenerations,
            regenerationCount + 1);
    }
}
