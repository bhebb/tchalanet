package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
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
