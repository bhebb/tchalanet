package com.tchalanet.server.core.sales.api.model.preparation;

import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;

public record SalePreparationPromotionLineView(
    String lineRef,
    String gameCode,
    String betType,
    Short betOption,
    String selection,
    TicketLineSelectionSource selectionSource,
    PromotionChoiceMode choiceMode,
    String promotionRuleKey,
    String promotionEffectType,
    boolean regenerable,
    int regenerationsRemaining) {}
