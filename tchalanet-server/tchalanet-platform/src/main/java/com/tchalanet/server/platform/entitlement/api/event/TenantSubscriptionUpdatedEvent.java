package com.tchalanet.server.platform.entitlement.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

public record TenantSubscriptionUpdatedEvent(EventId eventId,
                                             Instant occurredAt,
                                             TenantId tenantId,
                                             String reason
) implements DomainEvent {
}
