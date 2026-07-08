package com.tchalanet.server.core.drawresult.api.query.view;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.common.types.id.DrawResultId;
import java.time.Instant;
import java.time.LocalDate;

import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

public record DrawResultView(
    DrawResultId id,
    String slotKey,
    LocalDate resultDate,
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
) {
  // Normalize null -> empty object for consistent L2 cache round-trip (null JsonNode -> NullNode).
  public DrawResultView {
    sourceResult = sourceResult != null ? sourceResult : JsonNodeFactory.instance.objectNode();
    haitiResult = haitiResult != null ? haitiResult : JsonNodeFactory.instance.objectNode();
    rawPayload = rawPayload != null ? rawPayload : JsonNodeFactory.instance.objectNode();
  }
}
