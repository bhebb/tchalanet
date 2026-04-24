package com.tchalanet.server.core.session.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record SessionClosedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    SessionId sessionId,
    OutletId outletId,
    UserId cashierId,
    Instant openedAt,
    Instant closedAt,
    long netRevenueCents)
    implements DomainEvent {}
