package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListLastDaysDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListLastDaysDrawsHandler
    implements QueryHandler<ListLastDaysDrawsQuery, List<DrawSummary>> {

  private final DrawReaderPort drawReaderPort;

  @Override
  public List<DrawSummary> handle(ListLastDaysDrawsQuery query) {
    return drawReaderPort.findByCriteria(
        DrawSearchCriteria.lastDays(query.tenantId(), query.channelCode(), query.days()));
  }
}
