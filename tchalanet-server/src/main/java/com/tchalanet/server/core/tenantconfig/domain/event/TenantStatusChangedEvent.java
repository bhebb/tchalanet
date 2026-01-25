package com.tchalanet.server.core.tenantconfig.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: Tenant status changed.
 * Per core.tenant pattern: includes reason for audit trail.
 */
public record TenantStatusChangedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    TenantStatus fromStatus,
    TenantStatus toStatus,
    String reason  // optional reason for status change
) implements DomainEvent {}
