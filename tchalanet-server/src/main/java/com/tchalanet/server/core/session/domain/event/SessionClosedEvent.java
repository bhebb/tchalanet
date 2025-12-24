package com.tchalanet.server.core.session.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.Instant;
import java.util.UUID;

public record SessionClosedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID sessionId,
    UUID outletId,
    UUID cashierId,
    Instant openedAt,
    Instant closedAt,
    long netRevenueCents) implements DomainEvent {}

