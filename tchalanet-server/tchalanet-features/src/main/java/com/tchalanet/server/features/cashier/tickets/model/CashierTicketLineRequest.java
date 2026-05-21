package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CashierTicketLineRequest(
    @NotNull GameCode gameCode,
    @NotNull BetType betType,
    @NotBlank String selection,
    @Min(1) @Max(4) Short betOption,
    @NotNull @DecimalMin("0.01") BigDecimal stake
) {
}
