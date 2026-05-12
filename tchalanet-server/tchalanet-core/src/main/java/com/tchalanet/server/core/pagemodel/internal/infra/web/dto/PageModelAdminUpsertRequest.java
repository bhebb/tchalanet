package com.tchalanet.server.core.pagemodel.internal.infra.web.dto;

/**
 * DTO for admin upsert (create/update) of a PageModel.
 * Moved from features/pagemodel/admin/dto/ to core/pagemodel/infra/web/dto/.
 */
public record PageModelAdminUpsertRequest(
    String logicalId,
    String scope,
    String slug,
    Integer schemaVersion,
    Object model
) {}

