package com.tchalanet.server.features.pagemodel.application.admin;

import java.time.Instant;
import java.util.UUID;

public record PageModelDetailsDto(
    UUID id,
    UUID tenantId,
    String code,
    String lang,
    String json,
    Instant createdAt,
    Instant updatedAt) {}
