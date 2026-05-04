package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.model.DrawResultsResponse;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.HaitiResultResponse;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tools.jackson.databind.JsonNode;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface DrawAdminWebMapper {

    @Mapping(target = "id", expression = "java(summary.id().value().toString())")
    DrawSummaryResponse toDrawSummaryResponse(DrawSummary summary);

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
