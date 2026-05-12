package com.tchalanet.server.core.draw.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Publié quand un {@code Draw} tenant passe au statut {@code RESULTED}.
 * Signal tenant-scoped — consommé par {@code core.sales}, {@code features.stats} et le cache.
 *
 * @see com.tchalanet.server.core.drawresult.domain.event.DrawResultIngestedEvent événement global
 */
public record DrawResultAppliedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    DrawId drawId,
    LocalDate drawDate,
    ResultSlotId resultSlotId,
    DrawResultId drawResultId,
    DrawChannelId drawChannelId)
    implements DomainEvent {
}

