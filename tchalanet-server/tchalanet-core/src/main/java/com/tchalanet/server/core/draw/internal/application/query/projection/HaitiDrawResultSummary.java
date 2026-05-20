package com.tchalanet.server.core.draw.internal.application.query.projection;

import com.tchalanet.server.common.types.id.DrawResultId;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record HaitiDrawResultSummary(
    DrawResultId id,
    Instant occurredAt,
    String status,
    JsonNode haitiResult
) {
}
