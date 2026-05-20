package com.tchalanet.server.core.session.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.api.query.CashierIdentityView;
import com.tchalanet.server.core.session.api.query.GetCashierIdentityQuery;
import com.tchalanet.server.core.session.internal.application.port.out.CashierSessionDashboardReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCashierIdentityQueryHandler
    implements QueryHandler<GetCashierIdentityQuery, CashierIdentityView> {

    private final CashierSessionDashboardReaderPort reader;

    @Override
    public CashierIdentityView handle(GetCashierIdentityQuery query) {
        return reader.findIdentity(query.tenantId(), query.cashierId());
    }
}
