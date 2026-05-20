package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.query.CashierPendingApprovalView;
import com.tchalanet.server.core.sales.api.query.ListCashierPendingApprovalsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.CashierTicketDashboardReaderPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListCashierPendingApprovalsQueryHandler
    implements QueryHandler<ListCashierPendingApprovalsQuery, List<CashierPendingApprovalView>> {

    private final CashierTicketDashboardReaderPort reader;

    @Override
    public List<CashierPendingApprovalView> handle(ListCashierPendingApprovalsQuery query) {
        int limit = Math.min(Math.max(query.limit(), 1), 20);
        return reader.findPendingApprovals(query.cashierId(), limit);
    }
}
