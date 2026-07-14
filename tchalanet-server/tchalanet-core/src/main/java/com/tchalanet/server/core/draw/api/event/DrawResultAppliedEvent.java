package com.tchalanet.server.core.draw.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Publié quand un {@code Draw} tenant passe au statut {@code RESULTED}. Signal tenant-scoped —
 * consommé par {@code core.sales}, {@code core.analytics} et le cache.
 *
 * @see com.tchalanet.server.core.drawresult.internal.domain.event.DrawResultIngestedEvent événement
 *     global
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
    implements DomainEvent {}
