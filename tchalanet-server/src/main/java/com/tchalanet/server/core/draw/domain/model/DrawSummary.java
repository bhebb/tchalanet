package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.common.types.id.DrawId;

import java.time.Instant;
import java.time.LocalDate;


public record DrawSummary(
    DrawId id,

    DrawChannelSummary channel,
    ResultSlotSummary slot,

    LocalDate drawDate,
    Instant scheduledAt,
    Instant cutoffAt,

    DrawStatus status,
    boolean next,
    boolean active,

    HaitiDrawResultSummary lastResult
) {
}
