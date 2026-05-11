package com.tchalanet.server.core.ledger.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.query.model.LedgerEntryView;
import com.tchalanet.server.core.ledger.application.query.model.ListLedgerEntriesQuery;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListLedgerEntriesQueryHandler
    implements QueryHandler<ListLedgerEntriesQuery, TchPage<LedgerEntryView>> {

    private final LedgerReaderPort ledgerReader;

    @Override
    public TchPage<LedgerEntryView> handle(ListLedgerEntriesQuery query) {
        return ledgerReader.search(query);
    }
}
