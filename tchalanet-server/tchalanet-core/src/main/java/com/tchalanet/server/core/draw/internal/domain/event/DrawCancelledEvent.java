package com.tchalanet.server.core.draw.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Publié quand un {@code Draw} est annulé (statut {@code CANCELLED}).
 * Signal tenant-scoped — consommé par {@code core.sales}, {@code features.stats} et le cache.
 *
 * @see com.tchalanet.server.core.drawresult.internal.domain.event.DrawResultIngestedEvent événement global
 */
public record DrawCancelledEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    LocalDate drawDate,
    String reason)
    implements DomainEvent {
}

