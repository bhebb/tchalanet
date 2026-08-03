package com.tchalanet.server.core.sales.api.model.lifecycle;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.Money;
import java.time.Instant;
import java.util.Objects;

public record PaymentTrace(
    Instant paidAt,
    UserId paidBy,
    Money paidAmount,
    Instant adjustedAt,
    UserId adjustedBy,
    String adjustmentReason) {

  public PaymentTrace {
    if (paidAt == null) {
      throw new IllegalArgumentException("Payment date cannot be null");
    }
    if (paidBy == null) {
      throw new IllegalArgumentException("Payment actor cannot be null");
    }
    Objects.requireNonNull(paidAmount, "paid amount is required");
    if ((adjustedAt == null) != (adjustedBy == null)) {
      throw new IllegalArgumentException(
          "Payment adjustment actor and time must be provided together");
    }
    if (adjustedAt == null && adjustmentReason != null) {
      throw new IllegalArgumentException("Payment adjustment reason requires adjustment metadata");
    }
  }

  public static PaymentTrace of(Instant paidAt, UserId paidBy, Money paidAmount) {
    return new PaymentTrace(paidAt, paidBy, paidAmount, null, null, null);
  }

  public PaymentTrace adjusted(Money amount, Instant at, UserId by, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Payment adjustment reason is required");
    }
    return new PaymentTrace(paidAt, paidBy, amount, at, by, reason);
  }
}
