package com.tchalanet.server.core.sales.internal.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record TicketMoneyBreakdown(
    BigDecimal stakeAmount,
    BigDecimal feeAmount,
    BigDecimal totalAmount
) {
  public TicketMoneyBreakdown {
    Objects.requireNonNull(stakeAmount, "stakeAmount is required");
    Objects.requireNonNull(feeAmount, "feeAmount is required");
    Objects.requireNonNull(totalAmount, "totalAmount is required");

    if (stakeAmount.signum() < 0 || feeAmount.signum() < 0 || totalAmount.signum() < 0) {
      throw new IllegalArgumentException("money amounts must be >= 0");
    }
    if (stakeAmount.add(feeAmount).compareTo(totalAmount) != 0) {
      throw new IllegalArgumentException("totalAmount must equal stakeAmount + feeAmount");
    }
  }
}
