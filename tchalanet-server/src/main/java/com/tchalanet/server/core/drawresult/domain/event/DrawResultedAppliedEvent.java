package com.tchalanet.server.core.drawresult.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;

import java.time.Instant;
import java.time.LocalDate;

public record DrawResultedAppliedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    ResultSlotId resultSlotId,
    DrawResultId drawResultId,
    Instant drawResultOccurredAt,
    LocalDate drawDate)
    implements DomainEvent {
}
