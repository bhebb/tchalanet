package com.tchalanet.server.core.draw.internal.infra.web.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.draw.api.query.DrawResultSummary;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.internal.infra.web.model.DrawResultsResponse;
import com.tchalanet.server.core.draw.internal.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.internal.infra.web.model.HaitiDrawResultSummaryReponse;
import com.tchalanet.server.core.draw.internal.infra.web.model.HaitiResultResponse;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tools.jackson.databind.JsonNode;

@Mapper(
    componentModel = "spring",
    uses = {CommonIdMapper.class})
public interface DrawAdminWebMapper {

  @Mapping(target = "id", source = "drawId")
  @Mapping(target = "tenantId", expression = "java(drawSummary.tenantId().value().toString())")
  @Mapping(
      target = "channel.id",
      expression = "java(drawSummary.drawChannelId().value().toString())")
  @Mapping(target = "channel.code", source = "drawChannelCode")
  @Mapping(target = "channel.name", source = "drawChannelLabel")
  @Mapping(target = "slot.id", source = "resultSlotId")
  @Mapping(target = "slot.key", source = "resultSlotKey")
  @Mapping(target = "slot.label", source = "resultProvider")
  @Mapping(target = "slot.timezone", source = "resultTimezone")
  @Mapping(target = "slot.drawTime", source = "resultDrawTime")
  @Mapping(target = "lastResult", source = "result")
  @Mapping(target = "next", constant = "false")
  @Mapping(target = "active", source = "drawChannelActive")
  DrawSummaryResponse toDrawSummaryResponse(DrawSummary drawSummary);

  default HaitiDrawResultSummaryReponse toHaitiDrawResultSummaryReponse(DrawResultSummary result) {
    if (result == null) return null;

    Map<String, Object> haiti = result.haitiResult();
    return new HaitiDrawResultSummaryReponse(
        result.id().value().toString(),
        result.occurredAt(),
        result.status(),
        lotText(haiti, "lot1", "LOT1"),
        lotText(haiti, "lot2", "LOT2"),
        lotText(haiti, "lot3", "LOT3"),
        lotText(haiti, "lot4", "LOT4"));
  }

  @Mapping(target = "id", expression = "java(view.id().value().toString())")
  DrawResultsResponse toDrawResultsResponse(DrawResultView view);

  default HaitiResultResponse mapHaitiResult(JsonNode node) {
    if (node == null || node.isNull()) return null;

    return new HaitiResultResponse(
        node.path("lot1").asString(null),
        node.path("lot2").asString(null),
        node.path("lot3").asString(null),
        node.path("lot4").asString(null));
  }

  private static String text(Map<String, Object> payload, String key) {
    if (payload == null) return null;
    var value = payload.get(key);
    if (value == null) return null;
    var text = String.valueOf(value).trim();
    return text.isBlank() ? null : text;
  }

  @SuppressWarnings("unchecked")
  private static String lotText(Map<String, Object> payload, String flatKey, String nestedKey) {
    var flat = text(payload, flatKey);
    if (flat != null) return flat;
    if (payload == null) return null;
    if (!(payload.get("lots") instanceof Map<?, ?> lots)) return null;
    return text((Map<String, Object>) lots, nestedKey);
  }
}
