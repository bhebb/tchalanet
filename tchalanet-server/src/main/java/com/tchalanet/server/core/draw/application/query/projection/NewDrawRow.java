package com.tchalanet.server.core.draw.application.query.projection;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;

public record NewDrawRow(
    DrawId drawId,
    TenantId tenantId,
    DrawChannelId channelId,
    String channelCode,
    LocalDate drawDate,
    Instant scheduledAt,
    Instant cutoffAt,
    String status,
    String drawSource,
    boolean systemGenerated,
    boolean locked) {}
