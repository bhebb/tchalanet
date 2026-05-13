package com.tchalanet.server.core.sales.internal.domain.model;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.GameCode;
import java.math.BigDecimal;
import java.util.Objects;

public record TicketLine(
    int lineNo,
    GameCode gameCode,
    String selection,
    BetType betType,
    Short betOption,
    BigDecimal stakeAmount,
    BigDecimal oddsSnapshot,
    BigDecimal potentialPayoutAmount
) {
  public TicketLine {
    if (lineNo <= 0) throw new IllegalArgumentException("lineNo must be > 0");
    Objects.requireNonNull(gameCode, "gameCode is required");
    if (selection == null || selection.isBlank()) throw new IllegalArgumentException("selection is required");
    Objects.requireNonNull(betType, "betType is required");
    Objects.requireNonNull(stakeAmount, "stakeAmount is required");
    Objects.requireNonNull(oddsSnapshot, "oddsSnapshot is required");
    Objects.requireNonNull(potentialPayoutAmount, "potentialPayoutAmount is required");
  }

  // Helper for mapping where lineNo might not be known yet or is generated
  public TicketLine(GameCode gameCode, String selection, BigDecimal stakeAmount, BigDecimal oddsSnapshot, BigDecimal potentialPayoutAmount, BetType betType, Short betOption) {
    this(1, gameCode, selection, betType, betOption, stakeAmount, oddsSnapshot, potentialPayoutAmount);
  }

  public TicketLine(GameCode gameCode, String selection, BigDecimal stakeAmount, BigDecimal oddsSnapshot, BigDecimal potentialPayoutAmount, String betType, Short betOption) {
    this(gameCode, selection, stakeAmount, oddsSnapshot, potentialPayoutAmount, BetType.valueOf(betType), betOption);
  }

  public BigDecimal stake() {
    return stakeAmount;
  }

  public BigDecimal potentialPayout() {
    return potentialPayoutAmount;
  }
}
