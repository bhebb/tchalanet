package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import java.math.BigDecimal;
import java.util.Objects;

public record TicketLineCoverage(
    PricingVariantCode pricingVariantCode,
    Money stakeAmount,
    BigDecimal oddsSnapshot,
    Money settlementPayoutSnapshot,
    WinMode winMode
) {

    public TicketLineCoverage {
        Objects.requireNonNull(pricingVariantCode, "coverage.pricing_variant_required");
        Objects.requireNonNull(stakeAmount, "coverage.stake_required");
        Objects.requireNonNull(oddsSnapshot, "coverage.odds_required");
        Objects.requireNonNull(settlementPayoutSnapshot, "coverage.settlement_payout_required");
        winMode = winMode == null ? WinMode.ALTERNATIVE : winMode;

        if (!stakeAmount.currency().equals(settlementPayoutSnapshot.currency())) {
            throw new IllegalArgumentException("coverage.currency_mismatch");
        }
        if (oddsSnapshot.signum() <= 0) {
            throw new IllegalArgumentException("coverage.odds_must_be_positive");
        }
    }
}
