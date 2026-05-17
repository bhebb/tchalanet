package com.tchalanet.server.core.sales.api.model.lifecycle;


import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;

import java.time.Instant;

public record SettlementLifecycle(
    TicketSettlementStatus status,
    Instant settledAt,
    UserId settledBy,
    PaymentTrace payment
) {
    public static SettlementLifecycle notSettled() {
        return new SettlementLifecycle(TicketSettlementStatus.NOT_SETTLED, null, null, null);
    }

    public SettlementLifecycle settledPendingPayout(UserId by, Instant now) {
        return new SettlementLifecycle(TicketSettlementStatus.PAYOUT_PENDING, now, by, null);
    }

    public SettlementLifecycle settledWithoutPayout(UserId by, Instant now) {
        return new SettlementLifecycle(TicketSettlementStatus.NO_PAYOUT, now, by, null);
    }

    public SettlementLifecycle paid(PayoutId payoutId, UserId by, Instant at) {
        return new SettlementLifecycle(
            TicketSettlementStatus.PAID, settledAt, settledBy, new PaymentTrace(payoutId, at, by));
    }
}
