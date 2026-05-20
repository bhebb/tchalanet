package com.tchalanet.server.core.draw.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Publié quand un draw tenant remplace le {@code draw_result_id} déjà appliqué.
 *
 * <p>Important: cet événement décrit une correction/réapplication d'affectation
 * de résultat au niveau draw tenant, et non une correction globale de la source
 * de résultats externes.
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
