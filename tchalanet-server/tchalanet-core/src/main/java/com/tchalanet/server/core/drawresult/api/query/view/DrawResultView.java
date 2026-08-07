package com.tchalanet.server.core.drawresult.api.query.view;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import java.time.Instant;
import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

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
    Instant appliedAt,
    JsonNode sourceResult,
    JsonNode haitiResult,
    JsonNode rawPayload,
    String overrideReason) {
  // Normalize null -> empty object for consistent L2 cache round-trip (null JsonNode -> NullNode).
  public DrawResultView {
    sourceResult = sourceResult != null ? sourceResult : JsonUtils.emptyObject();
    haitiResult = haitiResult != null ? haitiResult : JsonUtils.emptyObject();
    rawPayload = rawPayload != null ? rawPayload : JsonUtils.emptyObject();
  }
}
