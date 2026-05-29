package com.tchalanet.server.core.sales.api.query.reconciliation;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.Money;

public record SalesOutcomeSummaryForDrawRow(
    DrawId drawId,
    long ticketCount,
    long acceptedTicketCount,
    long cancelledTicketCount,
    long voidedTicketCount,
    long wonTicketCount,
    long lostTicketCount,
    Money totalStake,
    Money totalPotentialPayout,
    Money totalWinningAmount
) {}
