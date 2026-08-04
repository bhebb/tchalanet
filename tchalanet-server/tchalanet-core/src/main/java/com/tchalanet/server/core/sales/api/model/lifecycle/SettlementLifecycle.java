package com.tchalanet.server.core.sales.api.model.lifecycle;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.time.Instant;

public record SettlementLifecycle(
    TicketSettlementStatus status, Instant settledAt, UserId settledBy, PaymentTrace payment) {
  public static SettlementLifecycle notSettled() {
    return new SettlementLifecycle(TicketSettlementStatus.NOT_SETTLED, null, null, null);
  }

  public SettlementLifecycle settledPendingPayout(UserId by, Instant now) {
    return new SettlementLifecycle(TicketSettlementStatus.PAYOUT_PENDING, now, by, null);
  }

  public SettlementLifecycle settledWithoutPayout(UserId by, Instant now) {
    return new SettlementLifecycle(TicketSettlementStatus.NO_PAYOUT, now, by, null);
  }

  public SettlementLifecycle paid(UserId by, Instant at, Money amount) {
    return new SettlementLifecycle(
        TicketSettlementStatus.PAID, settledAt, settledBy, PaymentTrace.of(at, by, amount));
  }

  public SettlementLifecycle adjusted(Money amount, UserId by, Instant at, String reason) {
    if (status != TicketSettlementStatus.PAID || payment == null) {
      throw new IllegalStateException("Ticket is not paid, cannot adjust paid amount");
    }
    return new SettlementLifecycle(
        status, settledAt, settledBy, payment.adjusted(amount, at, by, reason));
  }

  public SettlementLifecycle reversed(UserId by, Instant at) {
    return new SettlementLifecycle(TicketSettlementStatus.REVERSED, settledAt, settledBy, null);
  }
}
