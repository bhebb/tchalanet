package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public record CreateDrawChannelRequest(
    TenantId tenantId,
    String code,
    String name,
    String label,
    ZoneId timezone,
    LocalTime drawTime,
    Integer cutoffSec,
    List<DayOfWeek> daysOfWeek,
    boolean active,
    Integer sortOrder,
    String period,
    String notes,
    ResultSlotId resultSlotId,
    DrawSource defaultSource
) {}
