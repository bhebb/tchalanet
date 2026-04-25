package com.tchalanet.server.core.pagemodel.domain.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

/**
 * Application event émis lors de la mise à jour d'un PageModelTemplate par un admin platform.
 *
 * [Phase 4C] créé pour permettre la propagation des changements de template vers les instances
 * existantes (analysis §gap — PageModelTemplateUpdatedEvent absent).
 *
 * NOTE : cet event est un APPLICATION EVENT (pas un DomainEvent) — conforme à event_model.md §8 :
 * "le catalogue peut déclencher des application events (invalidate cache, refresh projections)
 *  mais jamais des domain events métier."
 *
 * PUBLICATION : en attente du GAP 4C-2 (aucun CommandHandler dans catalog/pagemodeltemplate —
 * seul PageModelTemplateAdminService (@Service) existe). L'événement sera publié depuis ce service
 * via AfterCommit + ApplicationEventPublisher dès que la migration vers un handler est effectuée.
 */
public record PageModelTemplateUpdatedEvent(
    PageModelTemplateId templateId,
    JsonNode newModel,
    int newSchemaVersion,
    UserId actorId,
    Instant occurredAt
) {}

