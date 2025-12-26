package com.tchalanet.server.features.pagemodel.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PageModelAdminListItemDto(
    UUID id,
    String logicalId,
    String scope,
    String slug,
    Integer schemaVersion,
    String title,
    List<String> langs,
    Instant updatedAt) {}
