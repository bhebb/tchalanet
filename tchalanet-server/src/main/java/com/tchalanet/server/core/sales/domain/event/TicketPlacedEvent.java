package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record TicketPlacedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID ticketId,
    UUID outletId,
    UUID cashierId,
    @Nullable UUID sessionId,
    UUID drawId,
    String gameCode,
    long stakeCents,
    String currencyCode) implements DomainEvent {}

