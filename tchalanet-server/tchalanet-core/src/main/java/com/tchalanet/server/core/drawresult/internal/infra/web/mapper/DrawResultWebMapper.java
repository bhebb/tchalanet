package com.tchalanet.server.core.drawresult.internal.infra.web.mapper;

import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.infra.web.model.DrawResultResponse;
import org.springframework.stereotype.Component;

@Component
public class DrawResultWebMapper {

    public DrawResultResponse toResponse(DrawResultView drawResultView) {
        if (drawResultView == null) return null;

        return new DrawResultResponse(
            drawResultView.occurredAt(),
            drawResultView.status(),
            drawResultView.source(),
            drawResultView.quality(),
            drawResultView.sourceHash(),
            drawResultView.fetchedAt(),
            drawResultView.sourceResult(),
            drawResultView.haitiResult(),
            drawResultView.rawPayload(),
            drawResultView.overrideReason());
    }

    public DrawResult toDomain(DrawResultResponse response) {
        if (response == null) return null;

        return new DrawResult(
            response.occurredAt(),
            response.status(),
            response.source(),
            response.quality(),
            response.sourceHash(),
            response.fetchedAt(),
            response.sourceResult(),
            response.haitiResult(),
            response.rawPayload(),
            response.overrideReason());
    }
}
