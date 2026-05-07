package com.tchalanet.server.core.outlet.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

/** Published after sales are blocked on an outlet. */
public record OutletSalesBlockedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OutletId outletId,
    String reason,
    UserId actorUserId)
    implements DomainEvent {}
