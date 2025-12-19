package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketResultedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID ticketId,
    BigDecimal totalPayout) implements DomainEvent {
}

