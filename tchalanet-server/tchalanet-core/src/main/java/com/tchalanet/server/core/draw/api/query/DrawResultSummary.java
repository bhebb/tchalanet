package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import java.time.Instant;
import java.util.Map;

public record DrawResultSummary(
    DrawResultId id,
    DrawResultStatus status,
    Instant occurredAt,
    Instant fetchedAt,
    String sourceHash,
    Map<String, Object> haitiResult) {}
