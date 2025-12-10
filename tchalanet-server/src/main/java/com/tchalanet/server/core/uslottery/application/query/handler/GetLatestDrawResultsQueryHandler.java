package com.tchalanet.server.core.uslottery.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.uslottery.application.query.model.GetLatestDrawResultsQuery;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetLatestDrawResultsQueryHandler implements QueryHandler<GetLatestDrawResultsQuery, List<DrawResult>> {

  private final DrawResultReaderPort drawResultReaderPort;
  private final Clock clock;

  @Override
  public List<DrawResult> handle(GetLatestDrawResultsQuery query) {
    ZonedDateTime now = ZonedDateTime.now(clock);
    var criteria = DrawResultsSearchCriteria.lastDays(query.tenantId(), query.channelCode(), now, query.days());
    var results = drawResultReaderPort.findByCriteria(criteria);
    log.debug("uslottery: returning {} results for tenant={} channel={}", results.size(), query.tenantId(), query.channelCode());
    return results;
  }
}

