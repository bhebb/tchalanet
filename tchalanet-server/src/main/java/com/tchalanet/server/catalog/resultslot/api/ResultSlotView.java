package com.tchalanet.server.catalog.resultslot.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.resultslot.internal.infra.persistence.ResultSlotJpaEntity;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

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

  public static ResultSlotView fromEntity(ResultSlotJpaEntity e) {
    Objects.requireNonNull(e);
    return new ResultSlotView(
        ResultSlotId.of(e.getId()),
        e.getSlotKey(),
        e.getProvider(),
        ZoneId.of(e.getTimezone()),
        e.getDrawTime(),
        e.getDaysOfWeek(),
        e.isActive(),
        e.getSourceCfg(),
        e.getProjectionCfg(),
        e.getLabelKey());
  }
}
