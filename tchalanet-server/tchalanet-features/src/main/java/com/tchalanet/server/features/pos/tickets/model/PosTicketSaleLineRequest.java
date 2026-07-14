package com.tchalanet.server.features.pos.tickets.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PosTicketSaleLineRequest(
    Integer lineNumber,
    @NotNull GameCode gameCode,
    @NotNull BetType betType,
    @NotBlank String selection,
    Short betOption,
    @JsonAlias("stake") @NotNull @DecimalMin(value = "0.0001") BigDecimal stakeAmount) {

  public SellTicketLineInput toLine(int fallbackLineNumber) {
    return new SellTicketLineInput(
        lineNumber == null || lineNumber < 1 ? fallbackLineNumber : lineNumber,
        gameCode,
        betType,
        selection,
        betOption,
        stakeAmount);
  }
}
