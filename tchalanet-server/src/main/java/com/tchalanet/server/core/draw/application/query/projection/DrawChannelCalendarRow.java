package com.tchalanet.server.core.draw.application.query.projection;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantGameId;
import java.time.LocalTime;

public record DrawChannelCalendarRow(
    DrawChannelId channelId,
    TenantGameId tenantGameId,
    String code,
    String timezone,
    LocalTime drawTime,
    int cutoffSec,
    String daysOfWeek,
    String defaultSource,
    boolean active,
    int sortOrder) {}
