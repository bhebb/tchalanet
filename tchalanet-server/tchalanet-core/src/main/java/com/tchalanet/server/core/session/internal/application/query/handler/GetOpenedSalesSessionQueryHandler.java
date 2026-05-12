package com.tchalanet.server.core.session.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.query.model.GetCurrentSalesSessionQuery;
import com.tchalanet.server.core.session.application.query.model.GetOpenedSalesSessionQuery;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@UseCase
@RequiredArgsConstructor
public class GetOpenedSalesSessionQueryHandler
    implements QueryHandler<GetOpenedSalesSessionQuery, List<SalesSession>> {

  private final SalesSessionReaderPort salesSessionReaderPort;

  @Override
  public List<SalesSession> handle(GetOpenedSalesSessionQuery query) {
    return salesSessionReaderPort.findOpenedSalesSession(query.tenantId(), query.terminalId(), query.outletId(), query.userId());
  }
}
