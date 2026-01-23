package com.tchalanet.server.core.drawresult.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DrawResultedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    LocalDate drawDate,
    String slotKey,
    UUID drawResultId)
    implements DomainEvent {}
