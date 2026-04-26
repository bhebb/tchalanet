package com.tchalanet.server.core.drawresult.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Publié quand un {@code DrawResult} global est ingéré (créé ou passe à FINAL).
 * Signal global, non lié à un tenant spécifique.
 *
 * @see com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent événement tenant-scoped
 */
public record DrawResultIngestedEvent(
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

