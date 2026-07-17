package com.tchalanet.server.core.sales.internal.infra.web.model;

import com.tchalanet.server.common.types.id.TicketId;

public record AdjustTicketPayoutPaidAmountResponse(
    TicketId ticketId,
    long calculatedAmountCents,
    long previousPaidAmountCents,
    long adjustedPaidAmountCents,
    long deltaAmountCents) {}
