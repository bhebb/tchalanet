package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListTodayDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListTodayDrawsHandler implements QueryHandler<ListTodayDrawsQuery, List<DrawSummary>> {

  private final DrawReaderPort drawReaderPort;

  @Override
  public List<DrawSummary> handle(ListTodayDrawsQuery query) {
    return drawReaderPort.findByCriteria(
        DrawSearchCriteria.today(query.tenantId(), query.channelCode()));
  }
}
