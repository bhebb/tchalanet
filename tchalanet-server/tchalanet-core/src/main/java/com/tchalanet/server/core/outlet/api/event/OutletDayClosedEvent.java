package com.tchalanet.server.core.outlet.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.outlet.api.command.lifecycle.CloseDayMode;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Published after an outlet business day is closed.
 *
 * <p>This is a cross-domain event: core.outlet publishes it, other domains
 * (core.session, etc.) listen to it without importing core.outlet.internal.
 */
public record OutletDayClosedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OutletId outletId,
    LocalDate closedDate,
    CloseDayMode mode,
    UserId actorUserId
) implements DomainEvent {
}
