package com.tchalanet.server.core.outlet.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

/** Published after a user is removed from an outlet (tenant_user.outlet_id set to null). */
public record OutletUserRemovedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OutletId outletId,
    UserId userId,
    UserId actorUserId)
    implements DomainEvent {}
