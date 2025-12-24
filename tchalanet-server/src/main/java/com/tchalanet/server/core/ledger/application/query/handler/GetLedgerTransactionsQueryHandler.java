package com.tchalanet.server.core.ledger.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.query.model.GetLedgerTransactionsQuery;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@UseCase
@Component
@RequiredArgsConstructor
public class GetLedgerTransactionsQueryHandler implements QueryHandler<GetLedgerTransactionsQuery, List<LedgerEntry>> {

    private final LedgerReaderPort ledgerReader;

    @Override
    public List<LedgerEntry> handle(GetLedgerTransactionsQuery query) {
        return ledgerReader.findByTenant(query.tenantId(), query.from(), query.to(), query.limit(), query.offset());
    }
}
