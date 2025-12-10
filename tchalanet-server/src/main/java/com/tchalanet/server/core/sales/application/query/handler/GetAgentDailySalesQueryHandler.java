package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.query.model.GetAgentDailySalesQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler stub that returns an agent daily sales summary. */
@UseCase
@RequiredArgsConstructor
@Component
public class GetAgentDailySalesQueryHandler implements QueryHandler<GetAgentDailySalesQuery, Map<String, Object>> {

  @Override
  public Map<String, Object> handle(GetAgentDailySalesQuery query) {
    // TODO: implement aggregation logic returning metrics (total sales, count, breakdown)
    throw new UnsupportedOperationException("GetAgentDailySalesQueryHandler not implemented yet");
  }
}

