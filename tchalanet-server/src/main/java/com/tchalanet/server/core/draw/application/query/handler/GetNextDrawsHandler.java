package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetNextDrawsHandler implements QueryHandler<GetNextDrawsQuery, List<Draw>> {

  private final DrawReaderPort drawReaderPort;

  @Override
  public List<Draw> handle(GetNextDrawsQuery query) {
    return drawReaderPort.findNextForChannels(query);
  }
}
