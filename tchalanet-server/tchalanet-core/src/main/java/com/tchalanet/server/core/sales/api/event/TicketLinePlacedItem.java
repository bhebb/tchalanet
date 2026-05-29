package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;

import java.math.BigDecimal;

/**
 * One ticket line as published inside {@link TicketPlacedEvent}.
 *
 * <p>Naming: this is a payload <em>item</em>, not a standalone event. The
 * {@code *Event} suffix is reserved for events published on the bus.
 */
public record TicketLinePlacedItem(
    TicketLineId lineId,
    int lineNumber,
    GameCode gameCode,
    BetType betType,
    String selectionKey,         // canonical key (stable across clients)
    String selectionDisplay,     // human-readable
    Short betOption,             // null when betType doesn't require one
    Money stakeAmount,
    BigDecimal oddsSnapshot,
    Money potentialPayoutAmount,
    TicketLineOrigin origin,
    TicketLinePricingSource pricingSource,
    TicketLineSelectionSource selectionSource,
    Money payoutBaseAmount,
    PromotionDecisionId promotionDecisionId,
    String promotionLabel,
    String promotionEffectType
) {
}
