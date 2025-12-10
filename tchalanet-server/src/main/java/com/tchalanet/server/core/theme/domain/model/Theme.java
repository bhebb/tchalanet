package com.tchalanet.server.core.theme.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Theme(
    UUID id,
    UUID tenantId,
    String basePresetId,
    String label,
    ThemeMode mode,
    short density,
    Map<String, Object> palette,
    Map<String, Object> tokens,
    Map<String, String> cssVars,
    ThemeStatus status,
    int version,
    Instant createdAt,
    Instant updatedAt) {}
