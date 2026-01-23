package com.tchalanet.server.catalog.resultslot.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.LocalTime;
import java.time.ZoneId;

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
    String labelKey) {}
