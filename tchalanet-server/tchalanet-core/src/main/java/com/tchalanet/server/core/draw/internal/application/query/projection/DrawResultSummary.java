package com.tchalanet.server.core.draw.internal.application.query.projection;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;

import java.time.Instant;
import java.util.Map;

public record DrawResultSummary(
    DrawResultId id,
    DrawResultStatus status,
    Instant occurredAt,
    String sourceHash,
    Map<String, Object> haitiResult
) {
}
