package com.tchalanet.server.core.user.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.Instant;
import java.util.UUID;

public record CashierCreatedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID userId,
    String username) implements DomainEvent {}

