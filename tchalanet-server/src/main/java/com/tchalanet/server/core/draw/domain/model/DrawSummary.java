package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.common.types.id.DrawId;
import tools.jackson.databind.JsonNode;

import java.time.ZonedDateTime;

public record DrawSummary(
    DrawId id,
    String channelCode,
    String channelName,
    ZonedDateTime scheduledAt,
    ZonedDateTime cutoffTime,
    DrawStatus status,
    boolean isNext,
    boolean active,
    JsonNode lastResult) {}
