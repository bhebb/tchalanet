package com.tchalanet.server.core.pagemodel.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

/**
 * Événement domaine émis après le reset d'un PageModel vers les valeurs du template.
 * Publie after-commit via AfterCommit.run() + DomainEventPublisher.
 *
 * Utilisation typique : invalider le cache BFF public de l'instance réinitialisée.
 * Conforme event_model.md §4 (after-commit) + §3.1 (changement d'état métier validé).
 */
public record PageModelResetEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    PageModelId id,
    UserId actorId
) implements DomainEvent {}

