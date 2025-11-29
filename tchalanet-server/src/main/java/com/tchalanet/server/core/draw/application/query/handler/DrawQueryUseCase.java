package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.core.draw.application.port.in.query.DrawQueryHandler;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.GetDrawQuery;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListDrawsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListLastDaysDrawsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListTodayDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawQueryUseCase implements DrawQueryHandler {

  private final DrawReaderPort drawReaderPort;

  @Override
  public Draw get(GetDrawQuery query) {
    return drawReaderPort
        .findById(query.tenantId(), query.drawId())
        .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + query.drawId()));
  }

  @Override
  public List<DrawSummary> list(ListDrawsQuery query) {
    return drawReaderPort.findByCriteria(
        new DrawSearchCriteria(query.tenantId(), query.channelCode(), query.from(), query.to()));
  }

  @Override
  public List<DrawSummary> listToday(ListTodayDrawsQuery query) {
    return drawReaderPort.findByCriteria(
        DrawSearchCriteria.today(query.tenantId(), query.channelCode()));
  }

  @Override
  public List<DrawSummary> listLastDays(ListLastDaysDrawsQuery query) {
    return drawReaderPort.findByCriteria(
        DrawSearchCriteria.lastDays(query.tenantId(), query.channelCode(), query.days()));
  }

  @Override
  public Draw getNext(GetNextDrawQuery query) {
    return drawReaderPort
        .findNext(query)
        .orElseThrow(() -> new IllegalArgumentException("No next draw for query " + query));
  }

  @Override
  public List<Draw> getNextForChannels(GetNextDrawsQuery query) {
    return drawReaderPort.findNextForChannels(query);
  }
}
