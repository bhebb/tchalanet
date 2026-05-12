package com.tchalanet.server.core.limitpolicy.internal.application.query.handler.assignment;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.api.query.GetLimitsOverviewQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitsOverviewView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetLimitsOverviewQueryHandler
    implements QueryHandler<GetLimitsOverviewQuery, LimitsOverviewView> {

    private final LimitAssignmentReaderPort assignmentReader;

    @Override
    public LimitsOverviewView handle(GetLimitsOverviewQuery query) {
        var assignments = assignmentReader.listByTarget(query.limitScopeRef()).stream()
            .filter(a -> !a.deleted())
            .map(a -> new LimitsOverviewView.Assignment(
                a.id(),
                a.ruleKey(),
                a.enabled(),
                a.onBreach(),
                a.params(),
                a.startsAt(),
                a.endsAt()
            ))
            .toList();

        return new LimitsOverviewView(
            query.limitScopeRef(),
            assignments
        );
    }
}
