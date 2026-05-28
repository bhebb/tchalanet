package com.tchalanet.server.catalog.pagemodeltemplate.api.model;

import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record PageModelTemplateView(
    PageModelTemplateId id,
    String code,
    String logicalId,
    String scope,
    String slug,
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
) {
    public static PageModelTemplateView initFromFile(
        String code,
        String logicalId,
        String scope,
        String slug,
        String name,
        String label,
        String description,
        JsonNode schema,
        JsonNode model,
        int schemaVersion,
        boolean isDefault,
        PageModelTemplateLevel level
    ) {
        return new PageModelTemplateView(
            null,
            code,
            logicalId,
            scope,
            slug,
            name,
            label,
            description,
            schema,
            model,
            schemaVersion,
            isDefault,
            level,
            null,
            null,
            null
        );
    }
}
