package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.view.SellerTerminalDailyStatsView;
import com.tchalanet.server.core.sales.api.query.GetSellerTerminalDailyStatsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalDailyStatsQueryHandler
    implements QueryHandler<GetSellerTerminalDailyStatsQuery, SellerTerminalDailyStatsView> {

    private final TicketProjectionReaderPort reader;

    @Override
    public SellerTerminalDailyStatsView handle(GetSellerTerminalDailyStatsQuery query) {
        var stats = reader.dailyStatsBySellerTerminal(
            query.sellerTerminalId(),
            query.tenantId(),
            query.from(),
            query.to()
        );
        return new SellerTerminalDailyStatsView(stats.ticketCount(), stats.salesTotalCents(), query.currency(), stats.breakdown());
    }
}
