package com.tchalanet.server.core.pagemodel.application.command.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Optional;

/**
 * Command for upsert (create or update) a PageModel.
 * [Phase 2A-1] tenantId rendu non-null et obligatoire (fourni par le controller via ctx).
 * [Phase 2A-1] actorId ajouté pour supprimer TchContext.get() du handler (analysis §BLOQUANT).
 * [Phase 2A-1] Command<PageModelInstance> au lieu de Command<Object> (analysis §MAJEUR).
 */
public record UpsertPageModelCommand(
    Optional<PageModelId> id,
    TenantId tenantId,
    UserId actorId,
    String logicalId,
    String scope,
    String slug,
    Integer schemaVersion,
    JsonNode modelJson,
    Optional<String> templateId
) implements Command<com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance> {}
