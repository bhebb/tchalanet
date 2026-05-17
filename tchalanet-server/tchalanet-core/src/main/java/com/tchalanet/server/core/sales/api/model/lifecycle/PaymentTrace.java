package com.tchalanet.server.core.sales.api.model.lifecycle;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record PaymentTrace(PayoutId payoutId, Instant paidAt, UserId paidBy) {

    public PaymentTrace {
        if (payoutId == null) {
            throw new IllegalArgumentException("payoutId is required");
        }
        if (paidAt == null) {
            throw new IllegalArgumentException("Payment date cannot be null");
        }
        if (paidBy == null) {
            throw new IllegalArgumentException("Payment actor cannot be null");
        }
    }

    public static PaymentTrace of(PayoutId payoutId, Instant paidAt, UserId paidBy){
        return new PaymentTrace(payoutId, paidAt, paidBy);
    }

}
