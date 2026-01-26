package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.drawresult.domain.model.DrawSource;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public record UpdateDrawChannelRequest(
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
    String notes,
    ResultSlotId resultSlotId,
    DrawSource defaultSource
) {}
