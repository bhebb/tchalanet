package com.tchalanet.server.core.draw.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;

import java.time.Instant;
import java.time.LocalDate;

public record DrawSettledEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    ResultSlotId resultSlotId,
    DrawResultId drawResultId,
    LocalDate drawDate,
    Instant scheduledAt
) implements DomainEvent {
}
