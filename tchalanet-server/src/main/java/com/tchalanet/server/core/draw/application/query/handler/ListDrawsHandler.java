package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.ListDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListDrawsHandler implements QueryHandler<ListDrawsQuery, List<DrawSummary>> {

  private final DrawLookupPort drawReaderPort;

  @Override
  public List<DrawSummary> handle(ListDrawsQuery query) {
    log.debug(
        "Handling ListDrawsQuery for tenantId={}, drawChannelCode={}, from={}, to={}",
        query.tenantId(),
        query.channelCode(),
        query.from(),
        query.to());

    return drawReaderPort.findByCriteria(
        DrawSearchCriteria.of(query.tenantId(), query.channelCode(), query.from(), query.to()));
  }
}
