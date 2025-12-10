package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletDailySummaryQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetOutletDailySummaryHandler implements QueryHandler<GetOutletDailySummaryQuery, Map<String,Object>> {

  @Override
  public Map<String,Object> handle(GetOutletDailySummaryQuery query) {
    // TODO: aggregate sales, payouts, ledger
    throw new UnsupportedOperationException("GetOutletDailySummaryHandler not implemented yet");
  }
}

