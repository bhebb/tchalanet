package com.tchalanet.server.core.drawresult.internal.application.view;

import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.id.DrawResultId;
import java.time.Instant;

import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import tools.jackson.databind.JsonNode;

public record DrawResultView(
    DrawResultId id,
    String slotKey,
    Instant occurredAt,
    DrawResultStatus status,
    DrawSource source,
    ResultQuality quality,
    String sourceHash,
    Instant fetchedAt,
    JsonNode sourceResult,
    JsonNode haitiResult,
    JsonNode rawPayload,
    String overrideReason
) {}
