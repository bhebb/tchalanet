package com.tchalanet.server.core.session.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.query.model.GetCurrentSalesSessionQuery;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentSessionQueryHandler
    implements QueryHandler<GetCurrentSalesSessionQuery, Optional<SalesSession>> {

  private final SalesSessionReaderPort salesSessionReaderPort;

  @Override
  public Optional<SalesSession> handle(GetCurrentSalesSessionQuery query) {
    return salesSessionReaderPort.findOpenByTerminal(query.tenantId(), query.terminalId());
  }
}
