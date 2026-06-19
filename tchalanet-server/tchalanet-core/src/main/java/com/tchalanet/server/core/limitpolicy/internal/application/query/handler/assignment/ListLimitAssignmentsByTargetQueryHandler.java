package com.tchalanet.server.core.limitpolicy.internal.application.query.handler.assignment;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListLimitAssignmentsByTargetQueryHandler
    implements QueryHandler<ListLimitAssignmentsByScopeQuery, ListLimitAssignmentsView> {

    private final LimitAssignmentReaderPort reader;

    @Override
    public ListLimitAssignmentsView handle(ListLimitAssignmentsByScopeQuery query) {
        var scope = toInternal(query.limitScopeRef());
        var items = reader.listByTarget(scope).stream()
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

    private LimitScopeRef toInternal(LimitScopeQueryRef ref) {
        return switch (ref.type()) {
            case TENANT -> LimitScopeRef.tenant(TenantId.of(ref.id()));
            case AGENT -> LimitScopeRef.agent(UserId.of(ref.id()));
            case SELLER_TERMINAL -> LimitScopeRef.sellerTerminal(SellerTerminalId.of(ref.id()));
            case DRAW_CHANNEL -> LimitScopeRef.drawChannel(DrawChannelId.of(ref.id()));
        };
    }
}
