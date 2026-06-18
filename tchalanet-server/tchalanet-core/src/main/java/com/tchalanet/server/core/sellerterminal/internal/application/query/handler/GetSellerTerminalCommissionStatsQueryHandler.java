package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalCommissionStatsQuery;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalCommissionStatsQueryHandler
    implements QueryHandler<GetSellerTerminalCommissionStatsQuery, SellerTerminalCommissionStatsView> {

    private final SellerTerminalReaderPort reader;

    @Override
    public SellerTerminalCommissionStatsView handle(GetSellerTerminalCommissionStatsQuery q) {
        return reader.commissionStats(q.tenantId(), q.tenantDefaultRate());
    }
}
