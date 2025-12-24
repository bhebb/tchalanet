package com.tchalanet.server.core.draw.application.query.projection;

import java.time.Instant;
import java.util.UUID;

public record NewDrawRow(
    UUID id,
    UUID tenantId,
    UUID drawChannelId,
    String gameCode,
    Instant scheduledAt,
    int cutoffSec,
    String status,
    String resultPayload,
    String drawSource,
    boolean systemGenerated,
    boolean locked) {}
