package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.query.model.GetDrawResultQuery;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetDrawResultHandler implements QueryHandler<GetDrawResultQuery, DrawResult> {

  private final DrawResultReaderPort drawResultReaderPort;

  @Override
  public DrawResult handle(GetDrawResultQuery query) {
    return drawResultReaderPort
        .findByDrawId(query.tenantId(), query.drawId())
        .orElseThrow(
            () -> new IllegalArgumentException("Draw result not found for draw " + query.drawId()));
  }
}
