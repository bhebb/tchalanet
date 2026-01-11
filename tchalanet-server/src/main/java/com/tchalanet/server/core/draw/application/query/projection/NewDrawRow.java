package com.tchalanet.server.core.draw.application.query.projection;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Write-optimized row used for bulk insert of draws.
 *
 * <p>Note: drawChannelCode is for logging/debug and read-model needs; insert uses drawChannelId.
 */
public record NewDrawRow(
    DrawId drawId,
    TenantId tenantId,
    DrawChannelId drawChannelId,
    LocalDate drawDate,
    Instant scheduledAt,
    Instant cutoffAt,
    String status,
    String drawSource,
    boolean systemGenerated,
    boolean locked) {}
