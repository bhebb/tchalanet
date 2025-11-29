package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawChannelSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListDrawChannelsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListDrawChannelsHandler
    implements QueryHandler<ListDrawChannelsQuery, List<DrawChannelSummary>> {

  private final DrawChannelReaderPort drawChannelReaderPort;

  @Override
  public List<DrawChannelSummary> handle(ListDrawChannelsQuery query) {
    var criteria = new DrawChannelSearchCriteria(query.tenantId(), query.activeOnly());
    return drawChannelReaderPort.findByCriteria(criteria);
  }
}
