package com.tchalanet.server.core.sales.api.command.payout;

import com.tchalanet.server.common.types.id.TicketId;

public record AdjustTicketPayoutPaidAmountResult(
    TicketId ticketId,
    long calculatedAmountCents,
    long previousPaidAmountCents,
    long adjustedPaidAmountCents,
    long deltaAmountCents) {}
