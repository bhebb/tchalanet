package com.tchalanet.server.core.draw.application.query.projection;

import java.time.LocalTime;
import java.util.UUID;

public record DrawChannelCalendarRow(
    UUID channelId,
    UUID tenantGameId,
    String code,
    String timezone,
    LocalTime drawTime,
    int cutoffSec,
    String daysOfWeek,
    String defaultSource,
    boolean active,
    int sortOrder) {}
