package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListLastDaysDrawResultsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListLastDaysDrawResultsHandler
    implements QueryHandler<ListLastDaysDrawResultsQuery, List<DrawResult>> {

  private final DrawResultReaderPort drawResultReaderPort;
  private final Clock clock;

  @Override
  public List<DrawResult> handle(ListLastDaysDrawResultsQuery query) {
    ZonedDateTime now = ZonedDateTime.now(clock);
    Integer page = query.page() == null ? 0 : query.page();
    Integer size = query.size() == null ? 50 : query.size();
    var criteria =
        DrawResultsSearchCriteria.lastDays(query.tenantId(), query.channelCode(), now, query.days())
            .withPageAndSize(page, size);
    return drawResultReaderPort.findByCriteria(criteria);
  }
}
