package com.tchalanet.server.catalog.resultslot.internal.web.model;

import tools.jackson.databind.JsonNode;

import java.time.LocalTime;

public record CreateResultSlotRequest(
    String slotKey,
    String provider,
    String timezone,
    LocalTime drawTime,
    String daysOfWeek,
    Integer sortOrder,
    JsonNode sourceCfg,
    JsonNode projectionCfg,
    String notes,
    String labelKey,
    Boolean active) implements com.tchalanet.server.catalog.resultslot.internal.web.model.BaseResultSlotRequest {}
