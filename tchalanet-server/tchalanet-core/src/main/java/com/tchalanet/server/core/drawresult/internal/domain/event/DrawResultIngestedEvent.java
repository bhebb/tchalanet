package com.tchalanet.server.core.drawresult.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Publié quand un {@code DrawResult} global est ingéré (créé ou passe à FINAL).
 * Signal global, non lié à un tenant spécifique.
 *
 * @see DrawResultAppliedEvent événement tenant-scoped
 */
public record DrawResultIngestedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId, // Always null for global ingestion
    ResultSlotId resultSlotId,
    String resultSlotKey,
    DrawResultId drawResultId,
    Instant drawResultOccurredAt,
    LocalDate drawDate)
    implements DomainEvent {
}

