package com.tchalanet.server.catalog.billing.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.catalog.billing.application.port.out.PlanReaderPort;
import com.tchalanet.server.catalog.billing.application.query.model.GetAvailablePlansQuery;
import com.tchalanet.server.catalog.billing.domain.model.Plan;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetAvailablePlansQueryHandler
    implements QueryHandler<GetAvailablePlansQuery, List<Plan>> {

  private final PlanReaderPort planReader;

  @Override
  public List<Plan> handle(GetAvailablePlansQuery query) {
    return planReader.findAllPublic();
  }
}
