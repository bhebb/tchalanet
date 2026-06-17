package com.tchalanet.server.core.terminal.internal.application.query.handler.sellerterminal;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.terminal.api.query.GetSellerTerminalCommissionStatsQuery;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
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
