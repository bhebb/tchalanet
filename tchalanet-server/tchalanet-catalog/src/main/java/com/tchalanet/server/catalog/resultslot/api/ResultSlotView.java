package com.tchalanet.server.catalog.resultslot.api;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.LocalTime;
import java.time.ZoneId;
import tools.jackson.databind.JsonNode;

/** Read-only view of a global Result Slot (cache-friendly). */
public record ResultSlotView(
    ResultSlotId id,
    String slotKey,
    String provider,
    ZoneId timezone,
    LocalTime drawTime,
    String daysOfWeek,
    boolean active,
    JsonNode sourceCfg,
    JsonNode projectionCfg,
    String labelKey) {

  // Normalize null -> empty object for consistent L2 cache round-trip (null JsonNode -> NullNode).
  public ResultSlotView {
    sourceCfg = sourceCfg != null ? sourceCfg : JsonUtils.emptyObject();
    projectionCfg = projectionCfg != null ? projectionCfg : JsonUtils.emptyObject();
  }
}
