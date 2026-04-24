package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitAssignmentsByTargetQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitAssignmentsView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListLimitAssignmentsByTargetQueryHandler
    implements QueryHandler<ListLimitAssignmentsByTargetQuery, ListLimitAssignmentsView> {

  private final LimitAssignmentReaderPort reader;

  @Override
  public ListLimitAssignmentsView handle(ListLimitAssignmentsByTargetQuery q) {
    var items =
        reader.listByTarget(q.target()).stream()
            .filter(a -> !a.isDeleted())
            .map(a -> new ListLimitAssignmentsView.Item(
                a.id(),
                a.limitDefinitionId(),
                a.enabled(),
                a.startsAt(),
                a.endsAt(),
                a.paramsOverride(),
                a.appliesToOverride()
            ))
            .toList();

    return new ListLimitAssignmentsView(q.target(), items);
  }
}
