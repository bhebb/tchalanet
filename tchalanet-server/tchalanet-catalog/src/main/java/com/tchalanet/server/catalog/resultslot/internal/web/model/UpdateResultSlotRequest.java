package com.tchalanet.server.catalog.resultslot.internal.web.model;

import java.time.LocalTime;
import tools.jackson.databind.JsonNode;

public record UpdateResultSlotRequest(
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
    Boolean active)
    implements BaseResultSlotRequest {}
