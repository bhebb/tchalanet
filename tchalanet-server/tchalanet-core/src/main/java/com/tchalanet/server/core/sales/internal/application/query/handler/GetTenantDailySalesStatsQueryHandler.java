package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.view.TenantDailySalesStatsView;
import com.tchalanet.server.core.sales.api.query.GetTenantDailySalesStatsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTenantDailySalesStatsQueryHandler
    implements QueryHandler<GetTenantDailySalesStatsQuery, TenantDailySalesStatsView> {

    private final TicketProjectionReaderPort reader;

    @Override
    public TenantDailySalesStatsView handle(GetTenantDailySalesStatsQuery query) {
        if (query.tenantId() == null) {
            return TenantDailySalesStatsView.empty(query.currency());
        }
        var stats = reader.dailyStatsByTenant(query.tenantId(), query.from(), query.to());
        return new TenantDailySalesStatsView(
            stats.ticketCount(),
            stats.salesTotalCents(),
            stats.activeSellerTerminals(),
            query.currency(),
            stats.gameBreakdown()
        );
    }
}
