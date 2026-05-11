package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import java.time.Instant;

public record TicketPayoutMarkedPaidEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    UserId paidBy,
    CurrencyCode currency,
    String source
) implements DomainEvent {}
