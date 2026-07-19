package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.draw.internal.application.exception.DrawNotFoundException;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.drawresult.api.error.DrawResultErrorCodes;
import com.tchalanet.server.core.drawresult.api.query.GetDrawResultProjectionByDrawIdQuery;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultProjection;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawResultProjectionByDrawIdQueryHandler
    implements QueryHandler<GetDrawResultProjectionByDrawIdQuery, DrawResultProjection> {

  private final DrawLookupPort drawReader;
  private final DrawResultReaderPort resultReader;

  @Override
  public DrawResultProjection handle(GetDrawResultProjectionByDrawIdQuery query) {

    var draw =
        drawReader
            .findById(query.drawId())
            .orElseThrow(() -> new DrawNotFoundException(query.drawId()));

    var drawResultId = draw.drawResultId();

    if (drawResultId == null) {
      throw ProblemRest.of(DrawResultErrorCodes.NOT_FOUND);
    }

    return resultReader
        .findProjectionById(drawResultId)
        .orElseThrow(() -> ProblemRest.of(DrawResultErrorCodes.NOT_FOUND));
  }
}
