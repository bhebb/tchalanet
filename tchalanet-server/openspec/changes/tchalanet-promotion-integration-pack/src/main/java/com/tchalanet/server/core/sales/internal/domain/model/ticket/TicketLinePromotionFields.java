package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;

/**
 * Value object to add to TicketLine. It groups promotion lifecycle dimensions.
 */
public record TicketLinePromotionFields(
    TicketLineOrigin origin,
    TicketLinePricingSource pricingSource,
    TicketLineSelectionSource selectionSource,
    PromotionDecisionId promotionDecisionId
) {
    public TicketLinePromotionFields {
        origin = origin == null ? TicketLineOrigin.CUSTOMER : origin;
        pricingSource = pricingSource == null ? TicketLinePricingSource.STANDARD : pricingSource;
        selectionSource = selectionSource == null ? TicketLineSelectionSource.CUSTOMER_SELECTED : selectionSource;
        if ((origin == TicketLineOrigin.PROMOTION || pricingSource == TicketLinePricingSource.PROMOTION)
            && promotionDecisionId == null) {
            throw new IllegalArgumentException("promotionDecisionId required for promotional line/pricing");
        }
    }

    public static TicketLinePromotionFields standard() {
        return new TicketLinePromotionFields(
            TicketLineOrigin.CUSTOMER,
            TicketLinePricingSource.STANDARD,
            TicketLineSelectionSource.CUSTOMER_SELECTED,
            null
        );
    }
}
