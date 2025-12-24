package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsByAgentQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsResult;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.domain.model.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@UseCase
@Component
@RequiredArgsConstructor
public class GetLimitAssignmentsByAgentQueryHandler implements QueryHandler<GetLimitAssignmentsByAgentQuery, GetLimitAssignmentsResult> {

    private final LimitAssignmentReaderPort assignmentReader;
    private final LimitDefinitionReaderPort limitDefinitionReader; // Add this line

    @Override
    public GetLimitAssignmentsResult handle(GetLimitAssignmentsByAgentQuery query) {
        var assignments = assignmentReader.findActiveByTarget(query.tenantId(), TargetType.AGENT.name(), query.agentId());

        var summaries = assignments.stream()
                .map(assignment -> {
                    Optional<LimitDefinition> limitOpt = limitDefinitionReader.findById(assignment.limitDefinitionId()); // Change this line
                    if (limitOpt.isEmpty()) {
                        // skip or handle
                        return null;
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
                            assignment.endsAt()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return new GetLimitAssignmentsResult(summaries);
    }
}
