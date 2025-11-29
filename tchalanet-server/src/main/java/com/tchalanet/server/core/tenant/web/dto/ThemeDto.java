package com.tchalanet.server.core.tenant.web.dto;

import com.tchalanet.server.core.tenant.domain.model.ThemeMode;
import com.tchalanet.server.core.tenant.domain.model.ThemeStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ThemeDto(
    UUID id,
    UUID tenantId,
    String basePresetId,
    String label,
    ThemeMode mode,
    Short density,
    Map<String, Object> palette,
    Map<String, Object> tokens,
    Map<String, String> cssVars,
    ThemeStatus status,
    Integer version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
