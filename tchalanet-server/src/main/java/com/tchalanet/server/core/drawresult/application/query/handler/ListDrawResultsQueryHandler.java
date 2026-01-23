package com.tchalanet.server.core.drawresult.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.api.DrawResultsCriteria;
import com.tchalanet.server.core.drawresult.application.query.model.ListDrawResultsQuery;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import com.tchalanet.server.catalog.drawresult.api.DrawResultReaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListDrawResultsQueryHandler
    implements QueryHandler<ListDrawResultsQuery, TchPage<DrawResult>> {

  private final DrawResultReaderPort drawResultReaderPort;

  @Override
  public TchPage<DrawResult> handle(ListDrawResultsQuery query) {
    var criteria =
        new DrawResultsCriteria(
            query.provider(), query.slotKey(), query.from(), query.to(), query.pageable());
    return drawResultReaderPort.findByCriteria(criteria);
  }
}
