package com.tchalanet.server.core.drawresult.infra.web.model;

import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record DrawResultResponse(
    Instant occurredAt,
    DrawResultStatus status,
    DrawSource source,
    ResultQuality quality,
    String sourceHash,
    Instant fetchedAt,
    JsonNode sourceResult,
    JsonNode haitiResult,
    JsonNode rawPayload,
    String overrideReason) {}
