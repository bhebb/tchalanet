package com.tchalanet.server.catalog.theme.api;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.ThemePresetId;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record ThemePresetView(
    ThemePresetId id,
    String code,
    String vendor,
    JsonNode config,
    String labelKey,
    boolean active,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt) {
  // Normalize null -> empty object for consistent L2 cache round-trip (null JsonNode -> NullNode).
  public ThemePresetView {
    config = config != null ? config : JsonUtils.emptyObject();
  }
}
