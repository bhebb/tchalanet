package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;
import java.util.UUID;

public record TicketPaymentPendingEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    String reason,
    UUID performedBy)
    implements DomainEvent {}
