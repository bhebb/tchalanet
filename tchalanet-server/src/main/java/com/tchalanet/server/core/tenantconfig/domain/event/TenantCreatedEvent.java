package com.tchalanet.server.core.tenantconfig.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: Tenant created.
 */
public record TenantCreatedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    String code
) implements DomainEvent {}
