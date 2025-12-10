package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.Instant;
import java.util.UUID;

public record TicketSettledEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID ticketId,
    UUID outletId,
    UUID cashierId,
    UUID drawId,
    String gameCode,
    long stakeCents,
    long winningsCents,
    String currencyCode) implements DomainEvent {}

