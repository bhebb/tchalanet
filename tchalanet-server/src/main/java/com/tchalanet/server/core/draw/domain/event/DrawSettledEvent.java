package com.tchalanet.server.core.draw.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record DrawSettledEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    DrawId drawId,
    String gameCode,
    Instant scheduledAt,
    String channelCode)
    implements DomainEvent {}
