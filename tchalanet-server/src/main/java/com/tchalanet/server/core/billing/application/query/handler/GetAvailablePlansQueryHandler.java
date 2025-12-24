package com.tchalanet.server.core.billing.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.billing.application.port.out.PlanReaderPort;
import com.tchalanet.server.core.billing.application.query.model.GetAvailablePlansQuery;
import com.tchalanet.server.core.billing.domain.model.Plan;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GetAvailablePlansQueryHandler implements QueryHandler<GetAvailablePlansQuery, List<Plan>> {

    private final PlanReaderPort planReader;

    @Override
    public List<Plan> handle(GetAvailablePlansQuery query) {
        return planReader.findAllPublic();
    }
}
