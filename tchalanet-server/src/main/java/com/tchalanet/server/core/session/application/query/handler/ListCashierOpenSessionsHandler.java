package com.tchalanet.server.core.session.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.PosSessionReaderPort;
import com.tchalanet.server.core.session.application.query.model.ListCashierOpenSessionsQuery;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListCashierOpenSessionsHandler implements QueryHandler<ListCashierOpenSessionsQuery, List<ListCashierOpenSessionsHandler.CashierSessionDto>> {

    private final PosSessionReaderPort sessionReader;

    public record CashierSessionDto(
        UUID sessionId,
        String channelCode,
        java.time.Instant openedAt,
        java.math.BigDecimal totalSales,
        long ticketsSold
    ) {}

    @Override
    public List<CashierSessionDto> handle(ListCashierOpenSessionsQuery query) {
        var sessions = sessionReader.findOpenByCashier(query.tenantId(), query.userId());
        return sessions.stream()
            .map(s -> new CashierSessionDto(
                s.id(),
                null, // channelCode not available
                s.openedAt(),
                s.totalStake(), // assuming totalStake is totalSales
                s.totalTickets() != null ? s.totalTickets() : 0L
            ))
            .collect(Collectors.toList());
    }
}
