package com.tchalanet.server.core.offlinesync.api.model.grant;

import com.tchalanet.server.common.types.money.Money;

/**
 * Offline quota tracked on a grant: hard caps + cumulative counters of TECH_VALIDATED tickets.
 */
public record GrantQuota(
    int maxTicketCount,
    Money maxTotalAmount,
    int consumedTicketCount,
    Money consumedTotalAmount
) {
    public GrantQuota {
        if (maxTicketCount <= 0)
            throw new IllegalArgumentException("maxTicketCount must be positive");
        if (consumedTicketCount < 0)
            throw new IllegalArgumentException("consumedTicketCount cannot be negative");
        if (!maxTotalAmount.currency().equals(consumedTotalAmount.currency()))
            throw new IllegalArgumentException("max/consumed currencies must match");
        if (consumedTotalAmount.amount().compareTo(maxTotalAmount.amount()) > 0)
            throw new IllegalArgumentException("consumed cannot exceed max amount");
    }

    public static GrantQuota initial(int maxTicketCount, Money maxTotalAmount) {
        return new GrantQuota(
            maxTicketCount, maxTotalAmount,
            0, Money.zero(maxTotalAmount.currency())
        );
    }

    public boolean canAccept(Money stake) {
        if (consumedTicketCount + 1 > maxTicketCount) return false;
        Money projected = consumedTotalAmount.plus(stake);
        return projected.amount().compareTo(maxTotalAmount.amount()) <= 0;
    }

    public GrantQuota recordValidated(Money stake) {
        if (!stake.currency().equals(maxTotalAmount.currency()))
            throw new IllegalArgumentException("currency mismatch");
        return new GrantQuota(
            maxTicketCount, maxTotalAmount,
            consumedTicketCount + 1,
            consumedTotalAmount.plus(stake)
        );
    }
}
