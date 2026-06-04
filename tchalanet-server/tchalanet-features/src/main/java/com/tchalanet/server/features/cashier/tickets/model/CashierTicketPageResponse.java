package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.time.Instant;

public record CashierTicketPageResponse(
    TicketId id,
    String ticketCode,
    String publicCode,
    TicketSaleStatus status,
    DrawId drawId,
    String drawChannelName,
    Instant drawScheduledAt,
    long totalAmountCents,
    String currency,
    Instant placedAt
) {}
