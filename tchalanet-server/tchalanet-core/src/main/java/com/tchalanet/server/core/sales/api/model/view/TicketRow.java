package com.tchalanet.server.core.sales.api.model.view;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.time.Instant;

public record TicketRow(
    TicketId id,
    String ticketCode,
    String publicCode,
    TicketSaleStatus status,
    DrawId drawId,
    SellerTerminalId sellerTerminalId,
    String drawChannelCode,
    String resultSlotKey,
    String resultProvider,
    String resultTimezone,
    String drawChannelName,
    Instant drawScheduledAt,
    long totalAmountCents,
    String currency,
    Instant placedAt) {}
