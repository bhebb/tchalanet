package com.tchalanet.server.core.ledger.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.query.model.GetLedgerBalanceQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@UseCase
@Component
@RequiredArgsConstructor
public class GetLedgerBalanceQueryHandler implements QueryHandler<GetLedgerBalanceQuery, BigDecimal> {

    private final LedgerReaderPort ledgerReader;

    @Override
    public BigDecimal handle(GetLedgerBalanceQuery query) {
        return ledgerReader.getBalance(query.tenantId());
    }
}
