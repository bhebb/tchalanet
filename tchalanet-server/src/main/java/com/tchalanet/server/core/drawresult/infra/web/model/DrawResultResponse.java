package com.tchalanet.server.catalog.drawresult.infra.web.model;

import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawSource;
import java.time.Instant;

public record DrawResultResponse(
    Instant occurredAt,
    DrawResultStatus status,
    DrawSource source,
    ResultQuality quality,
    String sourceHash,
    Instant fetchedAt,
    // JSON fields represented as strings in the HTTP API
    String sourceResult,
    String haitiResult,
    String rawPayload,
    String overrideReason) {}
