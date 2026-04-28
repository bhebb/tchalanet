package com.tchalanet.server.core.session.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.SalesSessionTotalsReaderPort;
import com.tchalanet.server.core.session.application.query.model.GetSessionTotalsQuery;
import com.tchalanet.server.core.session.domain.model.SalesSessionTotals;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSessionTotalsQueryHandler
    implements QueryHandler<GetSessionTotalsQuery, Optional<SalesSessionTotals>> {

  private final SalesSessionTotalsReaderPort totalsReader;

  @Override
  public Optional<SalesSessionTotals> handle(GetSessionTotalsQuery query) {
    return totalsReader.findBySessionId(query.sessionId());
  }
}
