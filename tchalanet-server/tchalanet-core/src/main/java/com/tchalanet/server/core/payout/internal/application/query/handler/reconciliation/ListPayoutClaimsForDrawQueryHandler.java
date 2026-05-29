package com.tchalanet.server.core.payout.internal.application.query.handler.reconciliation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.payout.api.query.reconciliation.ListPayoutClaimsForDrawQuery;
import com.tchalanet.server.core.payout.api.query.reconciliation.PayoutClaimForDrawRow;
import com.tchalanet.server.core.payout.internal.infra.persistence.SpringPayoutJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPayoutClaimsForDrawQueryHandler
    implements QueryHandler<ListPayoutClaimsForDrawQuery, List<PayoutClaimForDrawRow>> {

    private final QueryBus queryBus;
    private final SpringPayoutJpaRepository repository;

    @Override
    public List<PayoutClaimForDrawRow> handle(ListPayoutClaimsForDrawQuery query) {
        var tickets = PayoutReconciliationRows.ticketRows(queryBus, query.drawId());
        var ticketIds = tickets.keySet().stream().map(ticketId -> ticketId.value()).toList();
        return repository.findByTicketIdIn(ticketIds).stream()
            .map(payout -> {
                var ticket = tickets.get(PayoutReconciliationRows.ticketId(payout.getTicketId()));
                return new PayoutClaimForDrawRow(
                    PayoutId.of(payout.getId()),
                    ticket.ticketId(),
                    ticket.ticketCode(),
                    ticket.publicCode(),
                    ticket.displayCode(),
                    payout.getStatus(),
                    new Money(BigDecimal.valueOf(payout.getAmountCents(), 2), CurrencyCode.of(payout.getCurrency())),
                    payout.getOpenedAt(),
                    payout.getPaidAt(),
                    payout.getCancelledAt()
                );
            })
            .toList();
    }
}
