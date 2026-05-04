package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.query.model.GetDrawResultsQuery;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawResultsQueryHandler implements QueryHandler<GetDrawResultsQuery, DrawResultView> {

    private final DrawResultReaderPort drawResultReaderPort;

    @Override
    public DrawResultView handle(GetDrawResultsQuery query) {
        // TenantId est géré par RLS
        return drawResultReaderPort.findByDrawId(query.drawId())
            .orElseGet(() -> new DrawResultView(
                null, // id
                null, // slotKey
                null, // occurredAt
                DrawResultStatus.PROVISIONAL, // default status or similar
                null, null, null, null, null, null, null, null
            ));
    }
}
