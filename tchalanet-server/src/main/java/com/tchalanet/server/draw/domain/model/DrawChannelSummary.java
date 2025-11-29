package com.tchalanet.server.draw.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

public record DrawChannelSummary(
    String channelCode,
    String channelName,
    String status, // SCHEDULED, CLOSED, RESULTED
    OffsetDateTime drawTime,
    OffsetDateTime cutoffTime,
    boolean isNext,
    boolean active,
    List<Integer> lastResult) {}
