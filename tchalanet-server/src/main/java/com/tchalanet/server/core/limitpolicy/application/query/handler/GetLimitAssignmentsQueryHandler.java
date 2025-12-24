package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@Component
@RequiredArgsConstructor
public class GetLimitAssignmentsQueryHandler implements QueryHandler<GetLimitAssignmentsQuery, GetLimitAssignmentsResult> {

  private final LimitDefinitionReaderPort reader;

  @Override
  public GetLimitAssignmentsResult handle(GetLimitAssignmentsQuery query) {
    var assignments = reader.findActiveAssignmentsByTenantAndTarget(query.tenantId(), query.targetType(), query.targetId());
    return new GetLimitAssignmentsResult(assignments);
  }
}
