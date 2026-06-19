package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;

import java.time.Instant;

public record TicketWinningSettlementCreatedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    DrawId drawId,
    long amountCents,
    String currency,
    SellerTerminalId sellerTerminalId
) implements DomainEvent {}
