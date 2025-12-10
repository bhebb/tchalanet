package com.tchalanet.server.core.outlet.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.Instant;
import java.util.UUID;

public record OutletCreatedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID outletId,
    String name,
    String code) implements DomainEvent {}

