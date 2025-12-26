package com.tchalanet.server.core.tenant.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record TenantStatusChangedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    TenantStatus previousStatus,
    TenantStatus newStatus,
    String reason
) implements DomainEvent {
    @Override
    public String eventType() {
        return "tenant.status_changed";
    }
}
