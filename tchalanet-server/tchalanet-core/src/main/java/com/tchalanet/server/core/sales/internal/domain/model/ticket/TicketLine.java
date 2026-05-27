package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.selection.api.model.Selection;

import java.math.BigDecimal;
import java.util.Objects;

public record TicketLine(
    TicketLineId id,
    int lineNumber,
    GameCode gameCode,
    BetType betType,
    Selection selection,
    Money stakeAmount,
    Money payoutBaseAmount,
    BigDecimal oddsSnapshot,
    Money potentialPayoutAmount,
    Short betOption,
    TicketLineOrigin origin,
    TicketLinePricingSource pricingSource,
    TicketLineSelectionSource selectionSource,
    PromotionDecisionId promotionDecisionId,
    String promotionLabel,
    String promotionEffectType,
    TicketLineResultStatus resultStatus,
    Money payoutAmount
) {

    public TicketLine {
        if (id == null) {
            throw new IllegalArgumentException("ticket_line.id_required");
        }
        if (stakeAmount == null) {
            throw new IllegalArgumentException("ticket_line.stake_required");
        }
        if (payoutBaseAmount == null) {
            throw new IllegalArgumentException("ticket_line.payout_base_required");
        }
        if (oddsSnapshot == null) {
            throw new IllegalArgumentException("ticket_line.odds_required");
        }
        if (potentialPayoutAmount == null) {
            throw new IllegalArgumentException("ticket_line.potential_payout_required");
        }

        origin = origin == null ? TicketLineOrigin.CUSTOMER : origin;
        pricingSource = pricingSource == null ? TicketLinePricingSource.STANDARD : pricingSource;
        selectionSource = selectionSource == null
            ? TicketLineSelectionSource.CUSTOMER_SELECTED
            : selectionSource;

        if (origin == TicketLineOrigin.PROMOTION && promotionDecisionId == null) {
            throw new IllegalArgumentException("ticket_line.promotion_decision_required");
        }

        if (pricingSource == TicketLinePricingSource.PROMOTION && promotionDecisionId == null) {
            throw new IllegalArgumentException("ticket_line.promotion_pricing_requires_decision");
        }

        if (origin == TicketLineOrigin.CUSTOMER
            && pricingSource == TicketLinePricingSource.STANDARD
            && promotionDecisionId != null) {
            throw new IllegalArgumentException("ticket_line.standard_customer_line_cannot_have_promotion");
        }

        promotionLabel = normalizePromotionText(promotionLabel);
        promotionEffectType = normalizePromotionText(promotionEffectType);
    }

    public static TicketLine customerLine(
        TicketLineId id,
        int lineNumber,
        GameCode gameCode,
        BetType betType,
        Selection selection,
        Money stakeAmount,
        BigDecimal oddsSnapshot,
        Money potentialPayoutAmount,
        Short betOption,
        TicketLineResultStatus resultStatus,
        Money payoutAmount
    ) {
        return new TicketLine(
            id,
            lineNumber,
            gameCode,
            betType,
            selection,
            stakeAmount,
            stakeAmount, // payoutBaseAmount = stakeAmount for normal lines
            oddsSnapshot,
            potentialPayoutAmount,
            betOption,
            TicketLineOrigin.CUSTOMER,
            TicketLinePricingSource.STANDARD,
            TicketLineSelectionSource.CUSTOMER_SELECTED,
            null,
            null,
            null,
            resultStatus,
            payoutAmount
        );
    }

    public TicketLine withPromotionPricing(
        BigDecimal boostedOddsSnapshot,
        Money boostedPotentialPayout,
        PromotionDecisionId decisionId,
        String promotionLabel,
        String promotionEffectType
    ) {
        return new TicketLine(
            id,
            lineNumber,
            gameCode,
            betType,
            selection,
            stakeAmount,
            payoutBaseAmount,
            boostedOddsSnapshot,
            boostedPotentialPayout,
            betOption,
            origin,
            TicketLinePricingSource.PROMOTION,
            selectionSource,
            decisionId,
            promotionLabel,
            promotionEffectType,
            resultStatus,
            payoutAmount
        );
    }

    public TicketLine withResult(com.tchalanet.server.core.sales.api.model.line.TicketLineResult result) {
        if (result == null) throw new IllegalArgumentException("result.required");
        return new TicketLine(
            id,
            lineNumber,
            gameCode,
            betType,
            selection,
            stakeAmount,
            payoutBaseAmount,
            oddsSnapshot,
            potentialPayoutAmount,
            betOption,
            origin,
            pricingSource,
            selectionSource,
            promotionDecisionId,
            promotionLabel,
            promotionEffectType,
            result.status(),
            result.payoutAmount()
        );
    }

    public static TicketLine promotionLine(
        TicketLineId id,
        int lineNumber,
        GameCode gameCode,
        BetType betType,
        Selection selection,
        Money stakeAmount,
        Money payoutBaseAmount,
        BigDecimal oddsSnapshot,
        Money potentialPayoutAmount,
        Short betOption,
        TicketLineSelectionSource selectionSource,
        PromotionDecisionId promotionDecisionId,
        String promotionLabel,
        String promotionEffectType
    ) {
        return new TicketLine(
            id,
            lineNumber,
            gameCode,
            betType,
            selection,
            stakeAmount,
            payoutBaseAmount,
            oddsSnapshot,
            potentialPayoutAmount,
            betOption,
            TicketLineOrigin.PROMOTION,
            TicketLinePricingSource.PROMOTION,
            selectionSource,
            promotionDecisionId,
            promotionLabel,
            promotionEffectType,
            TicketLineResultStatus.PENDING,
            Money.zero(stakeAmount.currency())
        );
    }

    private static String normalizePromotionText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Objects.requireNonNull(value).trim();
    }
}
