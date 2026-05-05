package com.tchalanet.server.core.draw.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Publié quand le résultat d'un draw est corrigé.
 * Signal tenant-scoped — consommé pour invalider les caches et notifier les systèmes dépendants.
 */
public record DrawResultCorrectedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    DrawId drawId,
    LocalDate drawDate,
    ResultSlotId resultSlotId,
    DrawResultId previousDrawResultId,
    DrawResultId correctedDrawResultId,
    DrawChannelId drawChannelId,
    String reason)
    implements DomainEvent {
}
