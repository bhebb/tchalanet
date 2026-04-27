package com.tchalanet.server.core.draw.infra.web.model;

import java.time.OffsetDateTime;
import java.util.List;

public record DrawChannelSummaryResponse(
    String channelCode,
    String channelName,
    String status,
    OffsetDateTime drawTime,
    OffsetDateTime cutoffTime,
    boolean isNext,
    boolean active,
    List<Integer> lastResult) {}
