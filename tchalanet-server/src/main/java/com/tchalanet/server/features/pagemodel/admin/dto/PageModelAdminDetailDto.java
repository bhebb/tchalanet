package com.tchalanet.server.features.pagemodel.admin.dto;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import java.time.Instant;
import java.util.UUID;

public record PageModelAdminDetailDto(
    UUID id,
    UUID tenantId,
    String logicalId,
    String scope,
    String slug,
    Integer schemaVersion,
    PageModel model,
    Instant createdAt,
    Instant updatedAt) {}
