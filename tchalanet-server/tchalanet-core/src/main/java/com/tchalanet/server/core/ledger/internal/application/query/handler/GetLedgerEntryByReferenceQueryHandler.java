package com.tchalanet.server.core.ledger.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.query.model.GetLedgerEntryByReferenceQuery;
import com.tchalanet.server.core.ledger.application.query.model.LedgerEntryView;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetLedgerEntryByReferenceQueryHandler
    implements QueryHandler<GetLedgerEntryByReferenceQuery, Optional<LedgerEntryView>> {

    private final LedgerReaderPort ledgerReader;

    @Override
    public Optional<LedgerEntryView> handle(GetLedgerEntryByReferenceQuery query) {
        return ledgerReader
            .findByReferenceAndOperation(query.reference(), query.operationType())
            .map(this::toView);
    }

    private LedgerEntryView toView(LedgerEntry entry) {
        return new LedgerEntryView(
            entry.id(),
            entry.tenantId(),
            entry.reference().type(),
            entry.reference().id(),
            entry.operationType(),
            entry.amountCents(),
            entry.currency(),
            entry.direction(),
            entry.occurredAt(),
            entry.reversalOfEntryId(),
            entry.reason());
    }
}
