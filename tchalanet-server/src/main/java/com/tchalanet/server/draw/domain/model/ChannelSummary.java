package com.tchalanet.server.draw.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

public record ChannelSummary(
    String channelCode,
    String channelName,
    String status, // SCHEDULED, CLOSED, RESULTED
    OffsetDateTime drawTime,
    OffsetDateTime cutoffTime,
    boolean isNext,
    List<Integer> lastResult) {}
