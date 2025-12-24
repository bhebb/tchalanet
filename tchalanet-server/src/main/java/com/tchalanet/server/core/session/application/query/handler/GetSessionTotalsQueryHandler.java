package com.tchalanet.server.core.session.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.PosSessionTotalsReaderPort;
import com.tchalanet.server.core.session.application.query.model.GetSessionTotalsQuery;
import com.tchalanet.server.core.session.domain.model.PosSessionTotals;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSessionTotalsQueryHandler implements QueryHandler<GetSessionTotalsQuery, Optional<PosSessionTotals>> {

  private final PosSessionTotalsReaderPort totalsReader;

  @Override
  public Optional<PosSessionTotals> handle(GetSessionTotalsQuery query) {
    return totalsReader.findBySessionId(query.sessionId());
  }
}
