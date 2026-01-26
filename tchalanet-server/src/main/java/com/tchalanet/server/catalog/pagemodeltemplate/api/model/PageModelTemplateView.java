package com.tchalanet.server.catalog.pagemodeltemplate.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

public record PageModelTemplateView(
    PageModelTemplateId id,
    String code,
    String logicalId,
    String name,
    String label,
    String description,
    JsonNode schema,
    JsonNode model,
    Integer schemaVersion,
    boolean isDefault,
    PageModelTemplateLevel level,
    TenantId tenantId, // null when GLOBAL
    Instant createdAt,
    Instant updatedAt
) {}
