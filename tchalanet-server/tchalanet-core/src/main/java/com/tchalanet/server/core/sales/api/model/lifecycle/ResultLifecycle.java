package com.tchalanet.server.core.sales.api.model.lifecycle;


import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;

import java.time.Instant;
import java.util.Objects;

public record ResultLifecycle(
    TicketResultStatus status,
    Money winningAmount,
    Instant resultedAt,
    UserId resultedBy,
    String overrideReason
) {

    public ResultLifecycle {
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(winningAmount, "winningAmount is required");
    }
        public static ResultLifecycle notResulted(CurrencyCode currency) {
        return new ResultLifecycle(
            TicketResultStatus.NOT_RESULTED, Money.zero(currency), null, null, null);
    }

    public ResultLifecycle resulted(
        TicketResultStatus status, Money winning, UserId by, Instant now
    ) {
        return new ResultLifecycle(status, winning, now, by, null);
    }

    public ResultLifecycle overridden(
        TicketResultStatus status, Money winning, UserId by, String reason, Instant now
    ) {
        return new ResultLifecycle(status, winning, now, by, reason);
    }

        public Money totalPayoutAmount() {
            return winningAmount;
        }
}
