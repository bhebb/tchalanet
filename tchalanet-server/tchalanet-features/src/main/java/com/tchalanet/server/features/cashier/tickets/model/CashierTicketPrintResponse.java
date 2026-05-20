package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;

public record CashierTicketPrintResponse(
    TicketId id,
    String ticketCode,
    DrawId drawId,
    String gameName,
    long totalAmountCents,
    String currency,
    boolean printed,
    Instant printedAt,
    Instant placedAt
) {}
