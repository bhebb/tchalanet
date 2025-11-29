package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListTodayDrawResultQuery;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListTodayDrawResultsHandler
    implements QueryHandler<ListTodayDrawResultQuery, List<DrawResult>> {

  private final DrawResultReaderPort drawResultReaderPort;
  private final Clock clock;

  @Override
  public List<DrawResult> handle(ListTodayDrawResultQuery query) {
    ZonedDateTime now = ZonedDateTime.now(clock);
    var criteria = DrawResultsSearchCriteria.today(query.tenantId(), query.channelCode(), now);
    return drawResultReaderPort.findByCriteria(criteria);
  }
}
