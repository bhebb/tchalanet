package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.time.Instant;

public record PosTicketPageResponse(
    TicketId id,
    String ticketCode,
    String publicCode,
    TicketSaleStatus status,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    DrawId drawId,
    SellerTerminalId sellerTerminalId,
    String drawChannelCode,
    String resultSlotKey,
    String resultProvider,
    String resultTimezone,
    String drawChannelName,
    Instant drawScheduledAt,
    long totalAmountCents,
    long winningAmountCents,
    long paidAmountCents,
    String currency,
    Instant placedAt) {}
