package com.tchalanet.server.draw.application.query.handler;

import com.tchalanet.server.draw.application.port.in.query.DrawResultQueryHandler;
import com.tchalanet.server.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.draw.application.query.model.GetDrawResultQuery;
import com.tchalanet.server.draw.application.query.model.ListDrawResultsQuery;
import com.tchalanet.server.draw.application.query.model.ListLastDaysDrawResultsQuery;
import com.tchalanet.server.draw.application.query.model.ListTodayDrawResultQuery;
import com.tchalanet.server.draw.domain.model.DrawResult;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawQueryResultUseCase implements DrawResultQueryHandler {

  private final DrawResultReaderPort drawResultReaderPort;

  private final Clock clock;

  @Override
  public DrawResult get(GetDrawResultQuery query) {
    return drawResultReaderPort
        .findByDrawId(query.tenantId(), query.drawId())
        .orElseThrow(
            () -> new IllegalArgumentException("Draw result not found for draw " + query.drawId()));
  }

  @Override
  public List<DrawResult> list(ListDrawResultsQuery query) {
    ZonedDateTime now = ZonedDateTime.now(clock);
    var criteria =
        new DrawResultsSearchCriteria(
            query.tenantId(),
            query.channelCode(),
            query.from().atStartOfDay(now.getZone()),
            query.to().plusDays(1).atStartOfDay(now.getZone()));
    return drawResultReaderPort.findByCriteria(criteria);
  }

  @Override
  public List<DrawResult> listToday(ListTodayDrawResultQuery query) {
    ZonedDateTime now = ZonedDateTime.now(clock);
    var criteria = DrawResultsSearchCriteria.today(query.tenantId(), query.channelCode(), now);
    return drawResultReaderPort.findByCriteria(criteria);
  }

  @Override
  public List<DrawResult> listLastDays(ListLastDaysDrawResultsQuery query) {
    ZonedDateTime now = ZonedDateTime.now(clock);
    var criteria =
        DrawResultsSearchCriteria.lastDays(
            query.tenantId(), query.channelCode(), now, query.days());
    return drawResultReaderPort.findByCriteria(criteria);
  }
}
