package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawChannelSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListActiveDrawChannelsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListActiveDrawChannelsHandler
    implements QueryHandler<ListActiveDrawChannelsQuery, List<DrawChannelSummary>> {

  private final DrawChannelReaderPort drawChannelReaderPort;

  @Override
  public List<DrawChannelSummary> handle(ListActiveDrawChannelsQuery query) {
    var criteria =
        new DrawChannelSearchCriteria(
            query.tenantId(), true // only active
            );
    return drawChannelReaderPort.findByCriteria(criteria);
  }
}
