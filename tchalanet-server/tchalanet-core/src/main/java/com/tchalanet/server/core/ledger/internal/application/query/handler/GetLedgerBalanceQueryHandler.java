package com.tchalanet.server.core.ledger.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.internal.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.api.query.GetLedgerBalanceQuery;
import com.tchalanet.server.core.ledger.api.query.LedgerBalanceView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetLedgerBalanceQueryHandler
    implements QueryHandler<GetLedgerBalanceQuery, LedgerBalanceView> {

    private final LedgerReaderPort ledgerReader;

    @Override
    public LedgerBalanceView handle(GetLedgerBalanceQuery query) {
        return ledgerReader.getBalance(query);
    }
}
