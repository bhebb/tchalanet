package com.tchalanet.server.core.tenant.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;

import java.time.Instant;
import java.util.UUID;


public record TenantCreatedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    String tenantCode
) implements DomainEvent {

    @Override
    public String eventType() {
        return "tenant.created";
    }
}

