package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.query.CashierDashboardOverviewView;
import com.tchalanet.server.core.sales.api.query.GetCashierDashboardOverviewQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.CashierTicketDashboardReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCashierDashboardOverviewQueryHandler
    implements QueryHandler<GetCashierDashboardOverviewQuery, CashierDashboardOverviewView> {

    private final CashierTicketDashboardReaderPort reader;

    @Override
    public CashierDashboardOverviewView handle(GetCashierDashboardOverviewQuery query) {
        return reader.getOverview(query.tenantId(), query.cashierId(), query.businessDate());
    }
}
