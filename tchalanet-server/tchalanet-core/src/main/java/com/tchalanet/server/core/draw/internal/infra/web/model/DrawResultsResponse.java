package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import java.time.Instant;

public record DrawResultsResponse(
    String id,
    String slotKey,
    Instant occurredAt,
    DrawResultStatus status,
    DrawSource source,
    ResultQuality quality,
    String sourceHash,
    Instant fetchedAt,
    HaitiResultResponse haitiResult,
    String overrideReason
) {
    public record HaitiResultResponse(
        String lot1,
        String lot2,
        String lot3,
        String lot4
    ) {}
}
