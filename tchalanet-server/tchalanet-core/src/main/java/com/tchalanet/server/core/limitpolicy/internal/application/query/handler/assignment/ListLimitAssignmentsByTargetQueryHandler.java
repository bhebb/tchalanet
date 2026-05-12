package com.tchalanet.server.core.limitpolicy.internal.application.query.handler.assignment;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListLimitAssignmentsByTargetQueryHandler
    implements QueryHandler<ListLimitAssignmentsByScopeQuery, ListLimitAssignmentsView> {

    private final LimitAssignmentReaderPort reader;

    @Override
    public ListLimitAssignmentsView handle(ListLimitAssignmentsByScopeQuery query) {
        var items = reader.listByTarget(query.limitScopeRef()).stream()
            .filter(a -> !a.deleted())
            .map(a -> new ListLimitAssignmentsView.Item(
                a.id(),
                a.ruleKey(),
                a.enabled(),
                a.onBreach(),
                a.params(),
                a.startsAt(),
                a.endsAt()
            ))
            .toList();

        return new ListLimitAssignmentsView(query.limitScopeRef(), items);
    }
}
