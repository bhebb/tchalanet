package com.tchalanet.server.catalog.drawchannel.api.model;

import java.time.LocalTime;
import java.time.ZoneId;

public record DrawChannelSummaryView(
    String channelCode,
    String channelName,
    LocalTime drawTime,
    LocalTime cutoffTime,
    ZoneId timezone,
    boolean active) {
}
