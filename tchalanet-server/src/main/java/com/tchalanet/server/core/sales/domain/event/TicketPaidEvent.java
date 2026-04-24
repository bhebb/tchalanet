package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;
import java.util.UUID;

public record TicketPaidEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    UUID performedBy,
    String reason,
    long totalAmountCents,
    String currency)
    implements DomainEvent {}
