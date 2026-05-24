package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record DrawSummary(
    DrawId drawId,
    TenantId tenantId,
    LocalDate drawDate,
    DrawStatus status,
    Instant scheduledAt,
    Instant openedAt,
    Instant closedAt,
    Instant cutoffAt,
    Instant resultedAt,
    Instant settledAt,

    DrawChannelId drawChannelId,
    String drawChannelCode,
    String drawChannelLabel,
    LocalTime drawTime,
    String drawTimezone,
    boolean drawChannelActive,

    ResultSlotId resultSlotId,
    String resultSlotKey,
    String resultProvider,
    String resultTimezone,
    LocalTime resultDrawTime,
    boolean resultActive,

    DrawResultSummary result
) {}
