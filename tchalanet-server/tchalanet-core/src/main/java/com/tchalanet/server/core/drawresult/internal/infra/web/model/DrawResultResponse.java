package com.tchalanet.server.core.drawresult.internal.infra.web.model;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
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
