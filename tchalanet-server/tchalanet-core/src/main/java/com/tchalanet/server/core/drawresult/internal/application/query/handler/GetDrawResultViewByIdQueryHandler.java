package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.drawresult.api.error.DrawResultErrorCodes;
import com.tchalanet.server.core.drawresult.api.query.GetDrawResultViewByIdQuery;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawResultViewByIdQueryHandler
    implements QueryHandler<GetDrawResultViewByIdQuery, DrawResultView> {

  private final DrawResultReaderPort reader;

  @Override
  public DrawResultView handle(GetDrawResultViewByIdQuery query) {
    return reader
        .findViewById(query.id())
        .orElseThrow(() -> ProblemRest.of(DrawResultErrorCodes.NOT_FOUND));
  }
}
