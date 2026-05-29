package com.tchalanet.server.core.ledger.internal.application.query.handler.reconciliation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.api.query.reconciliation.GetLedgerSummaryForDrawQuery;
import com.tchalanet.server.core.ledger.api.query.reconciliation.LedgerSummaryForDrawRow;
import com.tchalanet.server.core.ledger.api.query.reconciliation.ListLedgerEntriesForDrawQuery;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerRefType;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetLedgerSummaryForDrawQueryHandler
    implements QueryHandler<GetLedgerSummaryForDrawQuery, LedgerSummaryForDrawRow> {

    private final QueryBus queryBus;

    @Override
    public LedgerSummaryForDrawRow handle(GetLedgerSummaryForDrawQuery query) {
        var entries = queryBus.ask(new ListLedgerEntriesForDrawQuery(query.drawId()));
        return new LedgerSummaryForDrawRow(
            query.drawId(),
            entries.size(),
            entries.stream().filter(e -> e.refType() == LedgerRefType.TICKET_SALE).count(),
            entries.stream().filter(e -> e.refType() == LedgerRefType.PAYOUT).count(),
            entries.stream().filter(e -> e.refType() == LedgerRefType.TICKET_SALE)
                .mapToLong(e -> e.amountCents()).sum(),
            entries.stream().filter(e -> e.refType() == LedgerRefType.PAYOUT)
                .mapToLong(e -> e.amountCents()).sum(),
            entries.isEmpty() ? "HTG" : entries.getFirst().currency()
        );
    }
}
