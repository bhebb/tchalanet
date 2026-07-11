package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.settlement.PayoutRuleSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementRuleCode;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSource;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermsSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementWinMode;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record TicketLine(
    TicketLineId id,
    int lineNumber,
    GameCode gameCode,
    BetType betType,
    Selection selection,
    Money stakeAmount,
    SettlementTermsSnapshot settlementTermsSnapshot,
    Short betOption,
    SelectionPolicy selectionPolicySnapshot,
    String betOptionLabelSnapshot,
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
        if (settlementTermsSnapshot == null) {
            throw new IllegalArgumentException("ticket_line.settlement_terms_required");
        }

        betOptionLabelSnapshot = normalizePromotionText(betOptionLabelSnapshot);
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

    public TicketLine(
        TicketLineId id,
        int lineNumber,
        GameCode gameCode,
        BetType betType,
        Selection selection,
        Money stakeAmount,
        Money payoutBaseAmount,
        BigDecimal oddsSnapshot,
        SettlementTermsSnapshot settlementTermsSnapshot,
        Short betOption,
        SelectionPolicy selectionPolicySnapshot,
        String betOptionLabelSnapshot,
        TicketLineOrigin origin,
        TicketLinePricingSource pricingSource,
        TicketLineSelectionSource selectionSource,
        PromotionDecisionId promotionDecisionId,
        String promotionLabel,
        String promotionEffectType,
        TicketLineResultStatus resultStatus,
        Money payoutAmount
    ) {
        this(
            id,
            lineNumber,
            gameCode,
            betType,
            selection,
            stakeAmount,
            settlementTermsSnapshot == null
                ? legacySettlementTermsSnapshot(
                    betType,
                    selection,
                    selectionPolicySnapshot,
                    betOptionLabelSnapshot,
                    betOption,
                    payoutBaseAmount,
                    oddsSnapshot)
                : settlementTermsSnapshot,
            betOption,
            selectionPolicySnapshot,
            betOptionLabelSnapshot,
            origin,
            pricingSource,
            selectionSource,
            promotionDecisionId,
            promotionLabel,
            promotionEffectType,
            resultStatus,
            payoutAmount
        );
    }

    public TicketLine(
        TicketLineId id,
        int lineNumber,
        GameCode gameCode,
        BetType betType,
        Selection selection,
        Money stakeAmount,
        Money payoutBaseAmount,
        BigDecimal oddsSnapshot,
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
        this(
            id,
            lineNumber,
            gameCode,
            betType,
            selection,
            stakeAmount,
            payoutBaseAmount,
            oddsSnapshot,
            null,
            betOption,
            null,
            null,
            origin,
            pricingSource,
            selectionSource,
            promotionDecisionId,
            promotionLabel,
            promotionEffectType,
            resultStatus,
            payoutAmount
        );
    }

    public static TicketLine customerLine(
        TicketLineId id,
        int lineNumber,
        GameCode gameCode,
        BetType betType,
        Selection selection,
        Money stakeAmount,
        BigDecimal oddsSnapshot,
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
            stakeAmount,
            oddsSnapshot,
            null,
            betOption,
            null,
            null,
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
            boostSettlementTerms(boostedOddsSnapshot),
            betOption,
            selectionPolicySnapshot,
            betOptionLabelSnapshot,
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
            settlementTermsSnapshot,
            betOption,
            selectionPolicySnapshot,
            betOptionLabelSnapshot,
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
        Short betOption,
        TicketLineSelectionSource selectionSource,
        PromotionDecisionId promotionDecisionId,
        String promotionLabel,
        String promotionEffectType
    ) {
        return promotionLine(
            id,
            lineNumber,
            gameCode,
            betType,
            selection,
            stakeAmount,
            payoutBaseAmount,
            oddsSnapshot,
            null,
            betOption,
            selectionSource,
            promotionDecisionId,
            promotionLabel,
            promotionEffectType
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
        SettlementTermsSnapshot settlementTermsSnapshot,
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
            settlementTermsSnapshot,
            betOption,
            null,
            null,
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

    private SettlementTermsSnapshot boostSettlementTerms(BigDecimal boostedOddsSnapshot) {
        var boostedTerms = settlementTermsSnapshot.terms().stream()
            .map(term -> new SettlementTermSnapshot(
                term.ruleCode(),
                term.sourceBetOption(),
                term.commercialLabel(),
                PayoutRuleSnapshot.stakeMultiplier(boostedOddsSnapshot),
                term.payoutBaseAmount(),
                term.winMode(),
                term.source()
            ))
            .toList();
        return new SettlementTermsSnapshot(
            settlementTermsSnapshot.schemaVersion(),
            settlementTermsSnapshot.selectionPolicy(),
            boostedTerms);
    }

    private static String normalizePromotionText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Objects.requireNonNull(value).trim();
    }

    private static SettlementTermsSnapshot legacySettlementTermsSnapshot(
        BetType betType,
        Selection selection,
        SelectionPolicy selectionPolicy,
        String betOptionLabel,
        Short betOption,
        Money payoutBaseAmount,
        BigDecimal oddsSnapshot
    ) {
        Objects.requireNonNull(payoutBaseAmount, "ticket_line.payout_base_required");
        Objects.requireNonNull(oddsSnapshot, "ticket_line.odds_required");
        var policy = selectionPolicy == null ? SelectionPolicy.EXPLICIT_ONLY : selectionPolicy;
        var term = new SettlementTermSnapshot(
            SettlementRuleCode.fromPricingVariant(
                com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver.resolve(
                    betType,
                    betOption,
                    selection == null ? null : selection.key().value()
                )
            ),
            policy == SelectionPolicy.IMPLICIT_BEST_MATCH ? null : betOption,
            policy == SelectionPolicy.IMPLICIT_BEST_MATCH ? null : betOptionLabel,
            PayoutRuleSnapshot.stakeMultiplier(oddsSnapshot),
            payoutBaseAmount.amount(),
            SettlementWinMode.ALTERNATIVE,
            SettlementTermSource.LEGACY_COVERAGE
        );
        return SettlementTermsSnapshot.current(policy, List.of(term));
    }
}
