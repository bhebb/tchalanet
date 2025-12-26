package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketResultedEvent(
    UUID eventId, Instant occurredAt, TenantId tenantId, TicketId ticketId, BigDecimal totalPayout)
    implements DomainEvent {}
