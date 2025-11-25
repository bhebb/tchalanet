package com.tchalanet.server.draw.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NextDrawDto(
    UUID drawId,
    UUID channelId,
    String channelCode,
    OffsetDateTime scheduledAt,
    OffsetDateTime cutoffAt,
    long serverTimeEpochMillis) {}
