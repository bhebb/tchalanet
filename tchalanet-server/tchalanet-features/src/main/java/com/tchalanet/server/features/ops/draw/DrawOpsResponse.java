package com.tchalanet.server.features.ops.draw;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.model.DrawStatus;

import java.time.Instant;
import java.time.LocalDate;

public record DrawOpsResponse(
    DrawId id,
    String tenantId,
    Channel channel,
    Slot slot,
    LocalDate drawDate,
    Instant scheduledAt,
    Instant cutoffAt,
    DrawStatus status,
    boolean active,
    Result lastResult
) {
    public record Channel(String id, String code, String name) {}

    public record Slot(String id, String key, String label, String timezone, String drawTime) {}

    public record Result(
        String id,
        Instant occurredAt,
        String status,
        String lot1,
        String lot2,
        String lot3,
        String lot4
    ) {}
}
