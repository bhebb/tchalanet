package com.tchalanet.server.core.pagemodel.application.command.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;

/** Command for upsert (create or update) a PageModel. */
public record UpsertPageModelCommand(
    Optional<PageModelId> id,
    Optional<TenantId> tenantId,
    String logicalId,
    String scope,
    String slug,
    Integer schemaVersion,
    JsonNode modelJson,
    Optional<String> templateId
) implements Command<Object> {}
