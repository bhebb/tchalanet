package com.tchalanet.server.core.sales.internal.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record TicketLine(
    int lineNo,
    String gameCode,
    String selection,
    String betType,
    Short betOption,
    BigDecimal stakeAmount,
    BigDecimal oddsSnapshot,
    BigDecimal potentialPayoutAmount
) {
  public TicketLine {
    if (lineNo <= 0) throw new IllegalArgumentException("lineNo must be > 0");
    if (gameCode == null || gameCode.isBlank()) throw new IllegalArgumentException("gameCode is required");
    if (selection == null || selection.isBlank()) throw new IllegalArgumentException("selection is required");
    if (betType == null || betType.isBlank()) throw new IllegalArgumentException("betType is required");
    Objects.requireNonNull(stakeAmount, "stakeAmount is required");
    Objects.requireNonNull(oddsSnapshot, "oddsSnapshot is required");
    Objects.requireNonNull(potentialPayoutAmount, "potentialPayoutAmount is required");
  }
}
