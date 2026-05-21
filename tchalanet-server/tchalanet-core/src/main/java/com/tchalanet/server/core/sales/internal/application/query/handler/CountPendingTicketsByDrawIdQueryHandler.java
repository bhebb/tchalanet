package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.application.query.model.CountPendingTicketsByDrawIdQuery;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CountPendingTicketsByDrawIdQueryHandler implements QueryHandler<CountPendingTicketsByDrawIdQuery, Long> {

    private final TicketJpaRepository ticketJpaRepository;

    @Override
    public Long handle(CountPendingTicketsByDrawIdQuery query) {
        List<TicketSettlementStatus> pendingStatuses = List.of(
            TicketSettlementStatus.NOT_SETTLED,
            TicketSettlementStatus.PAYOUT_PENDING
        );
        return ticketJpaRepository.countByDrawIdAndSettlementStatusIn(query.drawId().value(), pendingStatuses);
    }
}
