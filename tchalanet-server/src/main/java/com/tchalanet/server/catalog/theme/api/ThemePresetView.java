package com.tchalanet.server.catalog.theme.api;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

public record ThemePresetView(
    UUID id,
    String code,
    String vendor,
    JsonNode config,
    String labelKey,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
