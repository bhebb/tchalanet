package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetNextDrawHandler implements QueryHandler<GetNextDrawQuery, Draw> {

  private final DrawReaderPort drawReaderPort;

  @Override
  public Draw handle(GetNextDrawQuery query) {
    return drawReaderPort
        .findNext(query)
        .orElseThrow(() -> new IllegalArgumentException("No next draw for query " + query));
  }
}
