package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.coverage.PotentialGainMode;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.selection.api.model.Selection;

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
    Money potentialPayoutAmount,
    PotentialGainMode potentialGainMode,
    Money minPotentialGain,
    Money maxPotentialGain,
    Money totalPotentialGain,
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
        if (potentialGainMode == null) {
            throw new IllegalArgumentException("ticket_line.potential_gain_mode_required");
        }
        if (minPotentialGain == null) {
            throw new IllegalArgumentException("ticket_line.min_potential_gain_required");
        }
        if (maxPotentialGain == null) {
            throw new IllegalArgumentException("ticket_line.max_potential_gain_required");
        }
        if (!stakeAmount.currency().equals(minPotentialGain.currency())
            || !stakeAmount.currency().equals(maxPotentialGain.currency())
            || !stakeAmount.currency().equals(potentialPayoutAmount.currency())) {
            throw new IllegalArgumentException("ticket_line.currency_mismatch");
        }
        if (totalPotentialGain != null && !stakeAmount.currency().equals(totalPotentialGain.currency())) {
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
                potentialPayoutAmount,
                WinMode.ALTERNATIVE
            ));
        }
        validateCoverageSummary(
            potentialGainMode,
            minPotentialGain,
            maxPotentialGain,
            totalPotentialGain,
            coverages
        );

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
        this(
            id,
            lineNumber,
            gameCode,
            betType,
            selection,
            stakeAmount,
            payoutBaseAmount,
            oddsSnapshot,
            potentialPayoutAmount,
            PotentialGainMode.SINGLE,
            potentialPayoutAmount,
            potentialPayoutAmount,
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
            PotentialGainMode.SINGLE,
            potentialPayoutAmount,
            potentialPayoutAmount,
            null,
            List.of(new TicketLineCoverage(
                com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver.resolve(
                    betType,
                    betOption,
                    selection == null ? null : selection.key().value()
                ),
                stakeAmount,
                oddsSnapshot,
                potentialPayoutAmount,
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
            PotentialGainMode.SINGLE,
            boostedPotentialPayout,
            boostedPotentialPayout,
            null,
            List.of(new TicketLineCoverage(
                coverages.getFirst().pricingVariantCode(),
                payoutBaseAmount,
                boostedOddsSnapshot,
                boostedPotentialPayout,
                coverages.getFirst().winMode()
            )),
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
            potentialGainMode,
            minPotentialGain,
            maxPotentialGain,
            totalPotentialGain,
            coverages,
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
            PotentialGainMode.SINGLE,
            potentialPayoutAmount,
            potentialPayoutAmount,
            null,
            List.of(new TicketLineCoverage(
                com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver.resolve(
                    betType,
                    betOption,
                    selection == null ? null : selection.key().value()
                ),
                payoutBaseAmount,
                oddsSnapshot,
                potentialPayoutAmount,
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
        PotentialGainMode mode,
        Money minPotentialGain,
        Money maxPotentialGain,
        Money totalPotentialGain,
        List<TicketLineCoverage> coverages
    ) {
        if (coverages.stream().anyMatch(coverage ->
            !coverage.stakeAmount().currency().equals(minPotentialGain.currency())
                || !coverage.potentialGainSnapshot().currency().equals(minPotentialGain.currency()))) {
            throw new IllegalArgumentException("ticket_line.coverage_currency_mismatch");
        }
        if (minPotentialGain.amount().compareTo(maxPotentialGain.amount()) > 0) {
            throw new IllegalArgumentException("ticket_line.potential_gain_range_invalid");
        }

        var min = coverages.stream()
            .map(TicketLineCoverage::potentialGainSnapshot)
            .min(Comparator.comparing(Money::amount))
            .orElseThrow();
        var max = coverages.stream()
            .map(TicketLineCoverage::potentialGainSnapshot)
            .max(Comparator.comparing(Money::amount))
            .orElseThrow();

        if (min.amount().compareTo(minPotentialGain.amount()) != 0
            || max.amount().compareTo(maxPotentialGain.amount()) != 0) {
            throw new IllegalArgumentException("ticket_line.potential_gain_summary_mismatch");
        }

        if (mode == PotentialGainMode.RANGE_CUMULATIVE && totalPotentialGain == null) {
            throw new IllegalArgumentException("ticket_line.total_potential_gain_required");
        }
        if (mode != PotentialGainMode.RANGE_CUMULATIVE && totalPotentialGain != null) {
            throw new IllegalArgumentException("ticket_line.total_potential_gain_only_cumulative");
        }
    }
}
