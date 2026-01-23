package com.tchalanet.server.core.drawresult.infra.web.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.infra.web.model.DrawResultResponse;
import org.springframework.stereotype.Component;

@Component
public class DrawResultWebMapper {

  private final JsonUtils jsonUtils;

  public DrawResultWebMapper(JsonUtils jsonUtils) {
    this.jsonUtils = jsonUtils;
  }

  public DrawResultResponse toResponse(DrawResult drawResult) {
    if (drawResult == null) return null;
    String src =
        drawResult.sourceResult() == null ? null : jsonUtils.toJson(drawResult.sourceResult());
    String haiti =
        drawResult.haitiResult() == null ? null : jsonUtils.toJson(drawResult.haitiResult());
    String raw = drawResult.rawPayload() == null ? null : jsonUtils.toJson(drawResult.rawPayload());

    return new DrawResultResponse(
        drawResult.occurredAt(),
        drawResult.status(),
        drawResult.source(),
        drawResult.quality(),
        drawResult.sourceHash(),
        drawResult.fetchedAt(),
        src,
        haiti,
        raw,
        drawResult.overrideReason());
  }

  public DrawResult toDomain(DrawResultResponse response) {
    if (response == null) return null;
    JsonNode sourceResult =
        response.sourceResult() == null ? null : jsonUtils.parse(response.sourceResult());
    JsonNode haitiResult =
        response.haitiResult() == null ? null : jsonUtils.parse(response.haitiResult());
    JsonNode raw = response.rawPayload() == null ? null : jsonUtils.parse(response.rawPayload());

    return new DrawResult(
        response.occurredAt(),
        response.status(),
        response.source(),
        response.quality(),
        response.sourceHash(),
        response.fetchedAt(),
        sourceResult,
        haitiResult,
        raw,
        response.overrideReason());
  }
}
