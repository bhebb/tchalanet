package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;

import com.tchalanet.server.common.types.id.PromotionDecisionId;

public record TicketPrintLine(
    int lineNo,
    GameCode gameCode,
    BetType betType,
    Short betOption,
    String betOptionLabel,
    String gameLabel,
    String selectionRaw,
    String selectionCanonical,
    Money stake,
    SelectionPolicy selectionPolicySnapshot,
    TicketLineOrigin origin,
    TicketLinePricingSource pricingSource,
    TicketLineSelectionSource selectionSource,
    PromotionDecisionId promotionDecisionId,
    String promotionLabel,
    String promotionEffectType
) {
    public TicketPrintLine(
        int lineNo,
        GameCode gameCode,
        BetType betType,
        Short betOption,
        String gameLabel,
        String selectionRaw,
        String selectionCanonical,
        Money stake,
        SelectionPolicy selectionPolicySnapshot,
        TicketLineOrigin origin,
        TicketLinePricingSource pricingSource,
        TicketLineSelectionSource selectionSource,
        PromotionDecisionId promotionDecisionId,
        String promotionLabel,
        String promotionEffectType
    ) {
        this(
            lineNo,
            gameCode,
            betType,
            betOption,
            null,
            gameLabel,
            selectionRaw,
            selectionCanonical,
            stake,
            selectionPolicySnapshot,
            origin,
            pricingSource,
            selectionSource,
            promotionDecisionId,
            promotionLabel,
            promotionEffectType
        );
    }

    /** True when this line was added by a promotion (FREE_GAME_LINE). */
    public boolean isPromotionLine() {
        return origin == TicketLineOrigin.PROMOTION;
    }

    /** True when the odds on this line were boosted by a promotion. */
    public boolean isOdsBoosted() {
        return pricingSource == TicketLinePricingSource.PROMOTION
            && origin == TicketLineOrigin.CUSTOMER;
    }

    public boolean promotional() {
        return isPromotionLine() || isOdsBoosted() || promotionLabel != null;
    }
}
