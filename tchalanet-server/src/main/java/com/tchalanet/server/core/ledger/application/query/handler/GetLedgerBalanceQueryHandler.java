package com.tchalanet.server.core.ledger.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.query.model.GetLedgerBalanceQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetLedgerBalanceQueryHandler implements QueryHandler<GetLedgerBalanceQuery, Map<String, Object>> {

  @Override
  public Map<String, Object> handle(GetLedgerBalanceQuery query) {
    // TODO: implement balance retrieval (cash/bank/wallets)
    throw new UnsupportedOperationException("GetLedgerBalanceQueryHandler not implemented yet");
  }
}

