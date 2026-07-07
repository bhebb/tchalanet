package com.tchalanet.server.core.sales.api.model.preparation;

import com.tchalanet.server.core.sales.api.model.coverage.PotentialGainMode;
import java.math.BigDecimal;

public record SalePreparationLineView(
    int lineNumber,
    String gameCode,
    String betType,
    Short betOption,
    String selection,
    BigDecimal stakeAmount,
    BigDecimal oddsSnapshot,
    BigDecimal potentialPayoutAmount,
    PotentialGainMode potentialGainMode,
    BigDecimal minPotentialPayoutAmount,
    BigDecimal maxPotentialPayoutAmount,
    BigDecimal totalPotentialPayoutAmount,
    String origin
) {
    public SalePreparationLineView(
        int lineNumber,
        String gameCode,
        String betType,
        Short betOption,
        String selection,
        BigDecimal stakeAmount,
        BigDecimal oddsSnapshot,
        BigDecimal potentialPayoutAmount,
        String origin
    ) {
        this(
            lineNumber,
            gameCode,
            betType,
            betOption,
            selection,
            stakeAmount,
            oddsSnapshot,
            potentialPayoutAmount,
            PotentialGainMode.SINGLE,
            potentialPayoutAmount,
            potentialPayoutAmount,
            null,
            origin
        );
    }
}
