package com.tchalanet.server.core.draw.application.query.projection;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record NewDrawRow(
    DrawId id,
    TenantId tenantId,
    UUID drawChannelId,
    String gameCode,
    Instant scheduledAt,
    int cutoffSec,
    String status,
    String resultPayload,
    String drawSource,
    boolean systemGenerated,
    boolean locked) {}
