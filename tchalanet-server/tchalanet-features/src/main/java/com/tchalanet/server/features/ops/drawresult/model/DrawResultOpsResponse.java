package com.tchalanet.server.features.ops.drawresult.model;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record DrawResultOpsResponse(
    String id,
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
    String overrideReason) {}
