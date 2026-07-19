package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.drawresult.api.error.DrawResultErrorCodes;
import com.tchalanet.server.core.drawresult.api.query.GetDrawResultViewBySlotQuery;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawResultViewBySlotQueryHandler
    implements QueryHandler<GetDrawResultViewBySlotQuery, DrawResultView> {

  private final DrawResultReaderPort reader;

  @Override
  public DrawResultView handle(GetDrawResultViewBySlotQuery query) {
    return reader
        .findViewBySlotKeyAndOccurredAt(query.slotKey(), query.occurredAt())
        .orElseThrow(() -> ProblemRest.of(DrawResultErrorCodes.NOT_FOUND));
  }
}
