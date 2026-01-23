package com.tchalanet.server.catalog.theme.api;

import com.tchalanet.server.common.types.id.ThemePresetId;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record ThemePresetView(
    ThemePresetId id,
    String code,
    String vendor,
    JsonNode config,
    String labelKey,
    boolean active,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt
) {}
