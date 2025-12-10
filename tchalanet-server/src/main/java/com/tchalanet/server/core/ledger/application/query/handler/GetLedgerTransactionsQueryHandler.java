package com.tchalanet.server.core.ledger.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.query.model.GetLedgerTransactionsQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetLedgerTransactionsQueryHandler implements QueryHandler<GetLedgerTransactionsQuery, List<Object>> {

  @Override
  public List<Object> handle(GetLedgerTransactionsQuery query) {
    // TODO: implement paginated query
    throw new UnsupportedOperationException("GetLedgerTransactionsQueryHandler not implemented yet");
  }
}

