package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record TicketPayoutReversedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    DrawId drawId,
    long amountCents,
    String currency,
    SellerTerminalId sellerTerminalId,
    UserId reversedBy
) implements DomainEvent {}
