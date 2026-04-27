package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.common.types.id.DrawId;
import java.time.ZonedDateTime;
import java.util.List;

public record DrawSummary(
    DrawId id,
    String channelCode,
    String channelName,
    ZonedDateTime scheduledAt,
    ZonedDateTime cutoffTime,
    DrawStatus status,
    boolean isNext,
    boolean active,
    List<Integer> lastResult) {}
