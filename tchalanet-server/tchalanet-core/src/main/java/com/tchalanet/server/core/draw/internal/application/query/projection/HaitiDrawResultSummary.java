package com.tchalanet.server.core.draw.internal.application.query.projection;

import com.tchalanet.server.common.types.id.DrawResultId;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record HaitiDrawResultSummary(
    DrawResultId id, Instant occurredAt, String status, JsonNode haitiResult) {}
