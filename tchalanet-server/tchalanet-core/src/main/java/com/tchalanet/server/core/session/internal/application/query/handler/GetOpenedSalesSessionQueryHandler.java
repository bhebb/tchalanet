package com.tchalanet.server.core.session.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.api.query.GetOpenedSalesSessionQuery;
import com.tchalanet.server.core.session.api.query.OpenedSalesSessionView;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionReaderPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GetOpenedSalesSessionQueryHandler
    implements QueryHandler<GetOpenedSalesSessionQuery, List<OpenedSalesSessionView>> {

  private final SalesSessionReaderPort salesSessionReaderPort;

  @Override
  public List<OpenedSalesSessionView> handle(GetOpenedSalesSessionQuery query) {
    return salesSessionReaderPort
        .findOpenedSalesSession(query.tenantId(), query.terminalId(), query.outletId(), query.userId())
        .stream()
        .map(session -> new OpenedSalesSessionView(session.id(), session.openedAt()))
        .toList();
  }
}
