package com.tchalanet.server.core.sales.internal.application.query.handler.reconciliation;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.query.reconciliation.ActualTicketStateRow;
import com.tchalanet.server.core.sales.api.query.reconciliation.ListActualTicketStatesForDrawQuery;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketPublicCodeFormatter;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListActualTicketStatesForDrawQueryHandler
    implements QueryHandler<ListActualTicketStatesForDrawQuery, List<ActualTicketStateRow>> {

    private final TicketJpaRepository repository;
    private final TicketPublicCodeFormatter publicCodeFormatter;

    @Override
    public List<ActualTicketStateRow> handle(ListActualTicketStatesForDrawQuery query) {
        return repository.findWithLinesByDrawId(query.drawId().value()).stream()
            .map(ticket -> new ActualTicketStateRow(
                TicketId.of(ticket.getId()),
                ticket.getTicketCode(),
                ticket.getPublicCode(),
                publicCodeFormatter.display(ticket.getPublicCode()),
                ticket.getSaleStatus(),
                ticket.getResultStatus(),
                ticket.getSettlementStatus(),
                new Money(ticket.getPotentialPayoutAmount(), CurrencyCode.of(ticket.getCurrency())),
                ticket.getPlacedAt(),
                ticket.getSaleStatus() == TicketSaleStatus.CANCELLED,
                ticket.getSaleStatus() == TicketSaleStatus.VOIDED
            ))
            .toList();
    }
}
