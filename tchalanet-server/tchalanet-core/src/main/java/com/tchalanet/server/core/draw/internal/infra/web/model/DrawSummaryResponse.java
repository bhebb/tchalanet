package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.model.DrawStatus;

import java.time.Instant;
import java.time.LocalDate;


public record DrawSummaryResponse(
    DrawId id,

    DrawChannelSummaryResponse channel,
    ResultSlotSummaryResponse slot,

    LocalDate drawDate,
    Instant scheduledAt,
    Instant cutoffAt,

    DrawStatus status,
    boolean next,
    boolean active,

    HaitiDrawResultSummaryReponse lastResult
) {
}
