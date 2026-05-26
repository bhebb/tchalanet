package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;

import java.math.BigDecimal;

public record TicketPrintLine(
    int lineNo,
    GameCode gameCode,
    BetType betType,
    Short betOption,
    String gameLabel,
    String selectionRaw,
    String selectionCanonical,
    BigDecimal odds,
    Money stake,
    Money potentialPayout,
    TicketLineOrigin origin,
    TicketLinePricingSource pricingSource
) {
    /** True when this line was added by a promotion (FREE_GAME_LINE). */
    public boolean isPromotionLine() {
        return origin == TicketLineOrigin.PROMOTION;
    }

    /** True when the odds on this line were boosted by a promotion. */
    public boolean isOdsBoosted() {
        return pricingSource == TicketLinePricingSource.PROMOTION
            && origin == TicketLineOrigin.CUSTOMER;
    }
}
