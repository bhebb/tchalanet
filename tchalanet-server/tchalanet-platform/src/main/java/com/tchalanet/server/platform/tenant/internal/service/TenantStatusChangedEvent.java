package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

/**
 * Domain event: Tenant status changed.
 * Per core.tenant pattern: includes reason for audit trail.
 */
public record TenantStatusChangedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TenantStatus fromStatus,
    TenantStatus toStatus,
    String reason  // optional reason for status change
) implements DomainEvent {}
