package com.tchalanet.server.core.draw.application.query.projection;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import java.time.Instant;
import java.util.Map;

public record DrawResultSummary(
    DrawResultId id,
    String status,
    Instant occurredAt,
    String sourceHash,
    Map<String, Object> haitiResult
) {
}
