package com.tchalanet.server.core.draw.infra.web.model;

import com.tchalanet.server.common.types.id.DrawResultId;

import java.time.Instant;

public record HaitiDrawResultSummaryReponse(
    String id,
    Instant occurredAt,
    String status,
    String lot1,
    String lot2,
    String lot3,
    String lot4
) {
}
