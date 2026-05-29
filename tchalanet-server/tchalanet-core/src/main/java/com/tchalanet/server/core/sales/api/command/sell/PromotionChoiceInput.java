package com.tchalanet.server.core.sales.api.command.sell;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;

public record PromotionChoiceInput(
    PromotionDecisionId decisionId,
    String gameCode,
    int index,
    String rawSelection,
    TicketLineSelectionSource selectionSource
) {}
