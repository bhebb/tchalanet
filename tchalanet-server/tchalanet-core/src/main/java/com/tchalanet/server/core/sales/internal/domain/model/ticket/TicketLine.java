package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.coverage.SettlementPayoutMode;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
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
    Money settlementPayoutAmount,
    SettlementPayoutMode settlementPayoutMode,
    Money minSettlementPayout,
    Money maxSettlementPayout,
    Money totalSettlementPayout,
    List<TicketLineCoverage> coverages,
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
        if (payoutBaseAmount == null) {
            throw new IllegalArgumentException("ticket_line.payout_base_required");
        }
        if (oddsSnapshot == null) {
            throw new IllegalArgumentException("ticket_line.odds_required");
        }
        if (settlementPayoutAmount == null) {
            throw new IllegalArgumentException("ticket_line.settlement_payout_required");
        }
        if (settlementPayoutMode == null) {
            throw new IllegalArgumentException("ticket_line.settlement_payout_mode_required");
        }
        if (minSettlementPayout == null) {
            throw new IllegalArgumentException("ticket_line.min_settlement_payout_required");
        }
        if (maxSettlementPayout == null) {
            throw new IllegalArgumentException("ticket_line.max_settlement_payout_required");
        }
        if (!stakeAmount.currency().equals(minSettlementPayout.currency())
            || !stakeAmount.currency().equals(maxSettlementPayout.currency())
            || !stakeAmount.currency().equals(settlementPayoutAmount.currency())) {
            throw new IllegalArgumentException("ticket_line.currency_mismatch");
        }
        if (totalSettlementPayout != null && !stakeAmount.currency().equals(totalSettlementPayout.currency())) {
            throw new IllegalArgumentException("ticket_line.currency_mismatch");
        }
        coverages = coverages == null ? List.of() : List.copyOf(coverages);
        if (coverages.isEmpty()) {
            coverages = List.of(new TicketLineCoverage(
                com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver.resolve(
                    betType,
                    betOption,
                    selection == null ? null : selection.key().value()
                ),
                payoutBaseAmount,
                oddsSnapshot,
                settlementPayoutAmount,
                WinMode.ALTERNATIVE
            ));
        }
        validateCoverageSummary(
            settlementPayoutMode,
            minSettlementPayout,
            maxSettlementPayout,
            totalSettlementPayout,
            coverages
        );

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
        Money settlementPayoutAmount,
        SettlementPayoutMode settlementPayoutMode,
        Money minSettlementPayout,
        Money maxSettlementPayout,
        Money totalSettlementPayout,
        List<TicketLineCoverage> coverages,
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
            settlementPayoutAmount,
            settlementPayoutMode,
            minSettlementPayout,
            maxSettlementPayout,
            totalSettlementPayout,
            coverages,
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

    public TicketLine(
        TicketLineId id,
        int lineNumber,
        GameCode gameCode,
        BetType betType,
        Selection selection,
        Money stakeAmount,
        Money payoutBaseAmount,
        BigDecimal oddsSnapshot,
        Money settlementPayoutAmount,
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
            settlementPayoutAmount,
            SettlementPayoutMode.SINGLE,
            settlementPayoutAmount,
            settlementPayoutAmount,
            null,
            List.of(),
            betOption,
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
        Money settlementPayoutAmount,
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
            settlementPayoutAmount,
            SettlementPayoutMode.SINGLE,
            settlementPayoutAmount,
            settlementPayoutAmount,
            null,
            List.of(new TicketLineCoverage(
                com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver.resolve(
                    betType,
                    betOption,
                    selection == null ? null : selection.key().value()
                ),
                stakeAmount,
                oddsSnapshot,
                settlementPayoutAmount,
                WinMode.ALTERNATIVE
            )),
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
        Money boostedSettlementPayout,
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
            boostedSettlementPayout,
            SettlementPayoutMode.SINGLE,
            boostedSettlementPayout,
            boostedSettlementPayout,
            null,
            List.of(new TicketLineCoverage(
                coverages.getFirst().pricingVariantCode(),
                payoutBaseAmount,
                boostedOddsSnapshot,
                boostedSettlementPayout,
                coverages.getFirst().winMode()
            )),
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
            payoutBaseAmount,
            oddsSnapshot,
            settlementPayoutAmount,
            settlementPayoutMode,
            minSettlementPayout,
            maxSettlementPayout,
            totalSettlementPayout,
            coverages,
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
        Money settlementPayoutAmount,
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
            settlementPayoutAmount,
            SettlementPayoutMode.SINGLE,
            settlementPayoutAmount,
            settlementPayoutAmount,
            null,
            List.of(new TicketLineCoverage(
                com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver.resolve(
                    betType,
                    betOption,
                    selection == null ? null : selection.key().value()
                ),
                payoutBaseAmount,
                oddsSnapshot,
                settlementPayoutAmount,
                WinMode.ALTERNATIVE
            )),
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

    private static void validateCoverageSummary(
        SettlementPayoutMode mode,
        Money minSettlementPayout,
        Money maxSettlementPayout,
        Money totalSettlementPayout,
        List<TicketLineCoverage> coverages
    ) {
        if (coverages.stream().anyMatch(coverage ->
            !coverage.stakeAmount().currency().equals(minSettlementPayout.currency())
                || !coverage.settlementPayoutSnapshot().currency().equals(minSettlementPayout.currency()))) {
            throw new IllegalArgumentException("ticket_line.coverage_currency_mismatch");
        }
        if (minSettlementPayout.amount().compareTo(maxSettlementPayout.amount()) > 0) {
            throw new IllegalArgumentException("ticket_line.settlement_payout_range_invalid");
        }

        var min = coverages.stream()
            .map(TicketLineCoverage::settlementPayoutSnapshot)
            .min(Comparator.comparing(Money::amount))
            .orElseThrow();
        var max = coverages.stream()
            .map(TicketLineCoverage::settlementPayoutSnapshot)
            .max(Comparator.comparing(Money::amount))
            .orElseThrow();

        if (min.amount().compareTo(minSettlementPayout.amount()) != 0
            || max.amount().compareTo(maxSettlementPayout.amount()) != 0) {
            throw new IllegalArgumentException("ticket_line.settlement_payout_summary_mismatch");
        }

        if (mode == SettlementPayoutMode.RANGE_CUMULATIVE && totalSettlementPayout == null) {
            throw new IllegalArgumentException("ticket_line.total_settlement_payout_required");
        }
        if (mode != SettlementPayoutMode.RANGE_CUMULATIVE && totalSettlementPayout != null) {
            throw new IllegalArgumentException("ticket_line.total_settlement_payout_only_cumulative");
        }
    }
}
