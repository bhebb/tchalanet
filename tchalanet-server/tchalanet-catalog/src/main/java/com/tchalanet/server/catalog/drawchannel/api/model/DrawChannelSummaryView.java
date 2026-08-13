package com.tchalanet.server.catalog.drawchannel.api.model;

import java.time.LocalTime;
import java.time.ZoneId;

public record DrawChannelSummaryView(
    String id,
    String channelCode,
    String channelName,
    LocalTime drawTime,
    LocalTime cutoffTime,
    ZoneId timezone,
    String daysOfWeek,
    boolean active,
    boolean resultSlotActive,
    DrawSource defaultSource) {}
