package com.tchalanet.server.core.session.application.query.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.ports.in.ListCashierOpenSessionsQuery;
import com.tchalanet.server.core.pos.application.port.out.PosSessionRepositoryPort;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListCashierOpenSessionsHandler {

    private final PosSessionRepositoryPort sessionRepository;

    public record CashierSessionDto(
        UUID sessionId,
        String channelCode,
        java.time.Instant openedAt,
        java.math.BigDecimal totalSales,
        long ticketsSold
    ) {}

    public List<CashierSessionDto> handle(ListCashierOpenSessionsQuery query) {
        var sessions = sessionRepository.findByTenantIdAndUserIdAndStatus(query.tenantId(), query.userId(), PosSessionStatus.OPEN);
        return sessions.stream()
            .map(s -> new CashierSessionDto(
                s.getId(),
                null,
                s.getOpenedAt(),
                s.getTotalTicketsAmount(),
                s.getTotalTicketsAmount() != null ? s.getTotalTicketsAmount().longValue() : 0L
            ))
            .collect(Collectors.toList());
    }
}
