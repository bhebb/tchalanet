package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.application.query.ExistsPendingTicketsByDrawIdQuery;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExistsPendingTicketsByDrawIdQueryHandler implements QueryHandler<ExistsPendingTicketsByDrawIdQuery, Boolean> {

    private final TicketJpaRepository ticketJpaRepository;

    @Override
    public Boolean handle(ExistsPendingTicketsByDrawIdQuery query) {
        List<TicketSettlementStatus> pendingStatuses = List.of(
            TicketSettlementStatus.NOT_SETTLED,
            TicketSettlementStatus.PAYOUT_PENDING
        );
        return ticketJpaRepository.existsByDrawIdAndSettlementStatusIn(query.drawId().value(), pendingStatuses);
    }
}
