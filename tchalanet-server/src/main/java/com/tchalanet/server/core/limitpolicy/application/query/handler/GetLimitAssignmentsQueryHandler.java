package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsResult;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@Component
@RequiredArgsConstructor
public class GetLimitAssignmentsQueryHandler
    implements QueryHandler<GetLimitAssignmentsQuery, GetLimitAssignmentsResult> {

  private final LimitDefinitionReaderPort reader;

  @Override
  public GetLimitAssignmentsResult handle(GetLimitAssignmentsQuery query) {
    var assignments =
        reader.findActiveAssignmentsByTenantAndTarget(
            query.tenantId(), query.targetType(), query.targetId());

    var summaries =
        assignments.stream()
            .map(
                assignment -> {
                  Optional<com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition>
                      limitOpt = reader.findById(assignment.limitDefinitionId());
                  if (limitOpt.isEmpty()) {
                    return null; // skip if definition missing
                  }
                  var limit = limitOpt.get();
                  return new GetLimitAssignmentsResult.AssignmentSummary(
                      assignment.id(),
                      assignment.limitDefinitionId(),
                      limit.ruleKey(),
                      limit.enabled(),
                      limit.onBreach(),
                      limit.params(),
                      assignment.enabled(),
                      assignment.startsAt(),
                      assignment.endsAt());
                })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());

    return new GetLimitAssignmentsResult(summaries);
  }
}
