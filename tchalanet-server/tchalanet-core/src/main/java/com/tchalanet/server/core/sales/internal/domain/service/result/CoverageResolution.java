package com.tchalanet.server.core.sales.internal.domain.service.result;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.core.sales.api.model.coverage.SettlementPayoutMode;
import java.util.List;
import java.util.Objects;

public record CoverageResolution(
    BetOption option,
    SettlementPayoutMode settlementPayoutMode,
    List<CoverageVariant> variants
) {

    public CoverageResolution {
        Objects.requireNonNull(settlementPayoutMode, "coverage_resolution.settlement_payout_mode_required");
        variants = variants == null ? List.of() : List.copyOf(variants);
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("coverage_resolution.variants_required");
        }
    }
}
