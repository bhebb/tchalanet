package com.tchalanet.server.catalog.drawchannel.api.model;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record DrawChannelView(
    DrawChannelId id,
    TenantId tenantId,
    String code,
    String name,
    String label,
    ZoneId timezone,
    LocalTime drawTime,
    LocalTime salesOpenTime,
    Integer cutoffSec,
    List<DayOfWeek> daysOfWeek,
    boolean active,
    int sortOrder,
    String period,
    JsonNode flags,
    String notes,
    ResultSlotId resultSlotId,
    DrawSource defaultSource,
    String resultSlotKey,
    String resultProvider,
    String resultProviderSlotCode,
    String resultSlotDaysOfWeek,
    Boolean resultSlotActive,
    Instant createdAt,
    Instant updatedAt) {

  // Normalize null -> empty object for consistent L2 cache round-trip (null JsonNode -> NullNode).
  public DrawChannelView {
    flags = flags != null ? flags : JsonUtils.emptyObject();
  }
}
