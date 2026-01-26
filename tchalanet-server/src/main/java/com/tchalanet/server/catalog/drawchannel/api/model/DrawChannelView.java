package com.tchalanet.server.catalog.drawchannel.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.drawresult.domain.model.DrawSource;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public record DrawChannelView(
    DrawChannelId id,
    TenantId tenantId,
    String code,
    String name,
    String label,
    ZoneId timezone,
    LocalTime drawTime,
    Integer cutoffSec,
    List<DayOfWeek> daysOfWeek,
    boolean active,
    int sortOrder,
    JsonNode flags,
    String notes,
    ResultSlotId resultSlotId,
    DrawSource defaultSource,
    Instant createdAt,
    Instant updatedAt) {}
