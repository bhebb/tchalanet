package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.view.TerminalDailyStatsView;
import com.tchalanet.server.core.sales.api.query.GetTerminalDailyStatsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTerminalDailyStatsQueryHandler
    implements QueryHandler<GetTerminalDailyStatsQuery, TerminalDailyStatsView> {

    private final TicketProjectionReaderPort reader;

    @Override
    public TerminalDailyStatsView handle(GetTerminalDailyStatsQuery query) {
        var stats = reader.dailyStatsBySellerTerminal(
            query.sellerTerminalId(),
            query.tenantId(),
            query.from(),
            query.to()
        );
        return new TerminalDailyStatsView(stats.ticketCount(), stats.salesTotalCents(), query.currency(), stats.breakdown());
    }
}
