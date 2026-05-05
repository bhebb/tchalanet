package com.tchalanet.server.core.drawresult.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.application.exception.DrawNotFoundException;
import com.tchalanet.server.core.drawresult.application.exception.DrawResultNotFoundException;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultProjection;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.application.query.model.GetDrawResultProjectionByDrawIdQuery;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawResultProjectionByDrawIdQueryHandler
    implements QueryHandler<GetDrawResultProjectionByDrawIdQuery, DrawResultProjection> {

    private final DrawLookupPort drawReader;
    private final DrawResultReaderPort resultReader;

    @Override
    public DrawResultProjection handle(GetDrawResultProjectionByDrawIdQuery query) {

        var draw = drawReader.findById(query.drawId())
            .orElseThrow(() -> new DrawNotFoundException(query.drawId()));

        var drawResultId = draw.drawResultId();

        if (drawResultId == null) {
            throw new DrawResultNotFoundException("Draw has no result attached");
        }

        return resultReader
            .findProjectionById(drawResultId)
            .orElseThrow(() -> new DrawResultNotFoundException(
                "DrawResult not found for drawId=" + query.drawId()
            ));
    }
}
