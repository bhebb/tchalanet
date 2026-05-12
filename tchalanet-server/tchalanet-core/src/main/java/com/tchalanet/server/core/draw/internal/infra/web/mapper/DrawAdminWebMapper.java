package com.tchalanet.server.core.draw.internal.infra.web.mapper;

import com.tchalanet.server.common.json.mapper.CommonIdMapper;
import com.tchalanet.server.core.draw.application.query.projection.DrawResultSummary;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.model.DrawResultsResponse;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.HaitiDrawResultSummaryReponse;
import com.tchalanet.server.core.draw.infra.web.model.HaitiResultResponse;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface DrawAdminWebMapper {

    @Mapping(target = "id", source = "drawId")
    @Mapping(target = "channel.id", expression = "java(drawSummary.drawChannelId().value().toString())")
    @Mapping(target = "channel.code", source = "drawChannelCode")
    @Mapping(target = "channel.name", source = "drawChannelLabel")
    @Mapping(target = "slot.id", source = "resultSlotId")
    @Mapping(target = "slot.key", source = "resultSlotKey")
    @Mapping(target = "slot.label", source = "resultProvider")
    @Mapping(target = "slot.timezone", source = "resultTimezone")
    @Mapping(target = "slot.drawTime", source = "resultDrawTime")
    @Mapping(target = "lastResult", source = "result")
    @Mapping(target = "next", constant = "false") // Need logic if still used
    @Mapping(target = "active", source = "drawChannelActive")
    DrawSummaryResponse toDrawSummaryResponse(DrawSummary drawSummary);

    default HaitiDrawResultSummaryReponse toHaitiDrawResultSummaryReponse(DrawResultSummary result) {
        if (result == null) return null;

        Map<String, Object> haiti = result.haitiResult();
        return new HaitiDrawResultSummaryReponse(
            result.id().value().toString(),
            result.occurredAt(),
            result.status(),
            haiti != null ? (String) haiti.get("lot1") : null,
            haiti != null ? (String) haiti.get("lot2") : null,
            haiti != null ? (String) haiti.get("lot3") : null,
            haiti != null ? (String) haiti.get("lot4") : null
        );
    }

    @Mapping(target = "id", expression = "java(view.id().value().toString())")
    DrawResultsResponse toDrawResultsResponse(DrawResultView view);

    default HaitiResultResponse mapHaitiResult(JsonNode node) {
        if (node == null || node.isNull()) return null;

        return new HaitiResultResponse(
            node.path("lot1").asString(null),
            node.path("lot2").asString(null),
            node.path("lot3").asString(null),
            node.path("lot4").asString(null)
        );
    }

}
