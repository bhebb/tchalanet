package com.tchalanet.server.core.outlet.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.api.query.GetOutletAgentContextQuery;
import com.tchalanet.server.core.outlet.api.query.OutletAgentContextView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOutletAgentContextQueryHandler implements QueryHandler<GetOutletAgentContextQuery, OutletAgentContextView> {

    @Override
    public OutletAgentContextView handle(GetOutletAgentContextQuery query) {
        return new OutletAgentContextView();
    }
}
