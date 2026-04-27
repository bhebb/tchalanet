package com.tchalanet.server.core.sales.infra.web.model;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.GameCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SellTicketRequest(
    @NotNull UUID tenantId,
    @NotNull UUID terminalId,
    UUID sessionId,
    @NotNull UUID cashierId,
    @NotNull UUID drawId,
    @NotBlank String currency,
    @NotNull @Size(min = 1) @Valid List<LineRequest> lines
) {

    public record LineRequest(
        @NotNull GameCode gameCode,
        @NotBlank String selection,
        @NotNull @DecimalMin("0.01") BigDecimal stake,
        @NotNull BetType betType,

        // Nullable by default; constrained by assert below.
        @Min(1) @Max(3) Short betOption
    ) {

        @AssertTrue(message = "betOption is required (1..3) for LOTTO4_PATTERN/LOTTO5_PATTERN and must be null for other bet types")
        public boolean isBetOptionValidForBetType() {
            if (betType == null) return false; // validated elsewhere but keep safe
            if (betType.requiresBetOption()) {
                return betOption != null && betOption >= betType.betOptionMin() && betOption <= betType.betOptionMax();
            }
            return betOption == null;
        }
    }
}
