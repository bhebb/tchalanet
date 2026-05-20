package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;

import java.time.Instant;

public record HaitiDrawResultSummaryReponse(
    String id,
    Instant occurredAt,
    DrawResultStatus status,
    String lot1,
    String lot2,
    String lot3,
    String lot4
) {
}
