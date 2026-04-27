package com.tchalanet.server.core.draw.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

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
    ResultSlotId resultSlotId,
    DrawResultId drawResultId)
    implements DomainEvent {
}

