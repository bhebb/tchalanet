package com.tchalanet.server.core.draw.application.query.projection;

import java.time.Instant;
import java.util.UUID;

public record NewDrawRow(
    UUID tenantId,
    UUID drawChannelId,
    Instant scheduledAt,
    int cutoffSec,
    String status,
    String drawSource,
    boolean systemGenerated,
    boolean locked) {}
