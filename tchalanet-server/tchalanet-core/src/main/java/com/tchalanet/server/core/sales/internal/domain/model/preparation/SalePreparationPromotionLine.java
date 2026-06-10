package com.tchalanet.server.core.sales.internal.domain.model.preparation;

import java.math.BigDecimal;
import java.util.UUID;

public record SalePreparationPromotionLine(
    String lineRef,
    String gameCode,
    String betType,
    Short betOption,
    String selection,
    BigDecimal payoutBaseAmount,
    UUID promotionDecisionId,
    UUID promotionRuleId,
    boolean regenerable,
    int maxRegenerations,
    int regenerationCount
) {
    public SalePreparationPromotionLine withSelection(String newSelection) {
        return new SalePreparationPromotionLine(
            lineRef, gameCode, betType, betOption, newSelection, payoutBaseAmount,
            promotionDecisionId, promotionRuleId, regenerable, maxRegenerations,
            regenerationCount + 1);
    }
}
