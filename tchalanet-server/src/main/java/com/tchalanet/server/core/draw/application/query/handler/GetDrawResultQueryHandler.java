// ...existing code...
package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.query.model.GetDrawResultQuery;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@UseCase
@RequiredArgsConstructor
public class GetDrawResultQueryHandler
    implements QueryHandler<GetDrawResultQuery, Optional<DrawResultId>> {

  private final DrawResultReaderPort reader;

  @Override
  public Optional<DrawResultId> handle(GetDrawResultQuery q) {
    return reader.findByResultSlotIdAndOccurredAt(q.resultSlotId(), q.occurredAt());
  }
}
