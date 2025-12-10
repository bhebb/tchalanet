package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.query.model.GetSalesHistoryQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler stub for sales history queries. */
@UseCase
@RequiredArgsConstructor
@Component
public class GetSalesHistoryQueryHandler implements QueryHandler<GetSalesHistoryQuery, List<Object>> {

  @Override
  public List<Object> handle(GetSalesHistoryQuery query) {
    // TODO: implement search logic and return DTOs representing sales history
    throw new UnsupportedOperationException("GetSalesHistoryQueryHandler not implemented yet");
  }
}

