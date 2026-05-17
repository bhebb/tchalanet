package com.tchalanet.server.core.sales.internal.infra.web.model;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SellTicketLineRequest(
    @Min(1) int lineNumber,
    @NotNull GameCode gameCode,
    @NotNull BetType betType,
    @NotBlank String selection,
    @NotBlank Short betOption,
    @NotNull @DecimalMin(value = "0.0001") BigDecimal stakeAmount
) {
    public SellTicketLineInput toLine() {
        return new SellTicketLineInput(
            lineNumber,
            gameCode,
            betType,
            selection,
            betOption,
            stakeAmount
        );
    }
}
