package com.tchalanet.server.catalog.drawchannel.api.model;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantGameId;

public record GameSummaryView(
    TenantGameId tenantGameId,
    String gameCode,
    boolean enabled,
    JsonNode flags) {

  // Normalize null -> empty object so the value round-trips consistently through the L2 cache
  // (a null JsonNode comes back as NullNode after Redis serialization).
  public GameSummaryView {
    flags = flags != null ? flags : JsonUtils.emptyObject();
  }
}
