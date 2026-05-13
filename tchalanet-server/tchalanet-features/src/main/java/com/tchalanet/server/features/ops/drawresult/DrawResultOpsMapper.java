package com.tchalanet.server.features.ops.drawresult;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultProjection;
import com.tchalanet.server.core.drawresult.internal.application.view.DrawResultView;
import com.tchalanet.server.features.ops.drawresult.model.DrawResultOpsResponse;
import com.tchalanet.server.features.ops.drawresult.model.DrawResultProjectionOpsResponse;
import org.springframework.stereotype.Component;

@Component

public class DrawResultOpsMapper {

    public DrawResultOpsResponse toResponse(DrawResultView v) {
        return new DrawResultOpsResponse(
            v.id().value().toString(),
            v.slotKey(),
            v.occurredAt(),
            v.status(),
            v.source(),
            v.quality(),
            v.sourceHash(),
            v.fetchedAt(),
            v.sourceResult(),
            v.haitiResult(),
            v.rawPayload(),
            v.overrideReason()
        );
    }

    public DrawResultProjectionOpsResponse toProjectionResponse(DrawResultProjection p) {
        return new DrawResultProjectionOpsResponse(
            p.id().value().toString(),
            p.slotKey(),
            p.occurredAt(),
            p.lot1(),
            p.lot2(),
            p.lot3(),
            p.lot4(),
            p.derivedPairs()
        );
    }

    public TchPage<DrawResultOpsResponse> toPage(TchPage<DrawResultView> page) {
        var items = page.items().stream().map(this::toResponse).toList();

        return TchPage.of(
            items,
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            page.last(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
