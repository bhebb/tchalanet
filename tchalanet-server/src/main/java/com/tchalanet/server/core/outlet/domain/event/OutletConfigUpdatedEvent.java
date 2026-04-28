package com.tchalanet.server.core.outlet.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/** Published after an outlet configuration update is persisted. */
public record OutletConfigUpdatedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OutletId outletId)
    implements DomainEvent {}
