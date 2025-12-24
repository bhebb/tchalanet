package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.Instant;
import java.util.UUID;

public record TicketCancelledEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID ticketId,
    UUID terminalId,
    UUID sessionId,
    UUID performedBy,
    String reason,
    long totalStakeCents,
    String currency
) implements DomainEvent {}
