package com.tchalanet.server.core.session.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.api.query.CashierSessionSummaryView;
import com.tchalanet.server.core.session.api.query.GetCashierSessionSummaryQuery;
import com.tchalanet.server.core.session.internal.application.port.out.CashierSessionDashboardReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCashierSessionSummaryQueryHandler
    implements QueryHandler<GetCashierSessionSummaryQuery, CashierSessionSummaryView> {

    private final CashierSessionDashboardReaderPort reader;

    @Override
    public CashierSessionSummaryView handle(GetCashierSessionSummaryQuery query) {
        return reader.findActiveSessionSummary(query.tenantId(), query.cashierId())
            .orElse(CashierSessionSummaryView.inactive());
    }
}
