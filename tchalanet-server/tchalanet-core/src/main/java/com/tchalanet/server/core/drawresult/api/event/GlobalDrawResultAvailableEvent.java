package com.tchalanet.server.core.drawresult.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.drawresult.api.model.ResultSource;
import java.time.Instant;
import java.time.LocalDate;

public record GlobalDrawResultAvailableEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    ResultSlotId resultSlotId,
    String resultSlotKey,
    DrawResultId drawResultId,
    Instant drawResultOccurredAt,
    LocalDate drawDate,
    String providerCode,
    ResultSource source)
    implements DomainEvent {}
