package com.tchalanet.server.core.ledger.internal.application.query.handler.reconciliation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.ledger.api.query.reconciliation.LedgerEntryForDrawRow;
import com.tchalanet.server.core.ledger.api.query.reconciliation.ListLedgerEntriesForDrawQuery;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerRefType;
import com.tchalanet.server.core.ledger.internal.infra.persistence.LedgerEntryJpaEntity;
import com.tchalanet.server.core.ledger.internal.infra.persistence.LedgerEntryJpaRepository;
import com.tchalanet.server.core.payout.api.query.reconciliation.ListPayoutPaymentsForDrawQuery;
import com.tchalanet.server.core.sales.api.query.reconciliation.ActualTicketStateRow;
import com.tchalanet.server.core.sales.api.query.reconciliation.ListActualTicketStatesForDrawQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListLedgerEntriesForDrawQueryHandler
    implements QueryHandler<ListLedgerEntriesForDrawQuery, List<LedgerEntryForDrawRow>> {

    private final QueryBus queryBus;
    private final LedgerEntryJpaRepository repository;

    @Override
    public List<LedgerEntryForDrawRow> handle(ListLedgerEntriesForDrawQuery query) {
        var ticketRows = queryBus.ask(new ListActualTicketStatesForDrawQuery(query.drawId())).stream()
            .collect(Collectors.toMap(ActualTicketStateRow::ticketId, Function.identity()));
        var entries = new ArrayList<LedgerEntryForDrawRow>();

        entries.addAll(repository
            .findByRefTypeAndRefIdInAndDeletedAtIsNull(
                LedgerRefType.TICKET_SALE,
                ticketRows.keySet().stream().map(TicketId::value).toList())
            .stream()
            .map(entry -> toRow(entry, ticketRows.get(TicketId.of(entry.getRefId()))))
            .toList());

        var payments = queryBus.ask(new ListPayoutPaymentsForDrawQuery(query.drawId()));
        var paymentTicketByPayoutId = payments.stream()
            .collect(Collectors.toMap(payment -> payment.payoutId().value(), payment -> payment.ticketId()));
        entries.addAll(repository
            .findByRefTypeAndRefIdInAndDeletedAtIsNull(
                LedgerRefType.PAYOUT,
                new ArrayList<>(paymentTicketByPayoutId.keySet()))
            .stream()
            .map(entry -> toRow(entry, ticketRows.get(paymentTicketByPayoutId.get(entry.getRefId()))))
            .toList());

        return List.copyOf(entries);
    }

    private LedgerEntryForDrawRow toRow(LedgerEntryJpaEntity entry, ActualTicketStateRow ticket) {
        return new LedgerEntryForDrawRow(
            LedgerEntryId.of(entry.getId()),
            entry.getRefType(),
            entry.getRefId(),
            ticket == null ? null : ticket.ticketId(),
            ticket == null ? null : ticket.ticketCode(),
            ticket == null ? null : ticket.publicCode(),
            ticket == null ? null : ticket.displayCode(),
            entry.getOperationType(),
            entry.getAmountCents(),
            entry.getCurrency(),
            entry.getDirection(),
            entry.getOccurredAt()
        );
    }
}
