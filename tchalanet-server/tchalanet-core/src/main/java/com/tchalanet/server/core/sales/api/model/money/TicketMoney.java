package com.tchalanet.server.core.sales.api.model.money;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;

import java.util.Objects;

public record TicketMoney(
    CurrencyCode currency,
    TicketMoneyBreakdown breakdown
) {
    public TicketMoney {
        Objects.requireNonNull(currency, "currency is required");
        Objects.requireNonNull(breakdown, "breakdown is required");

        if (!breakdown.total().currency().equals(currency)) {
            throw new IllegalArgumentException("Breakdown currency mismatch");
        }
        if (!breakdown.stake().currency().equals(currency)) {
            throw new IllegalArgumentException("Stake currency mismatch");
        }
    }

    public Money stakeAmount() {
        return breakdown.stake();
    }

    public Money totalAmount() {
        return breakdown.total();
    }

}
