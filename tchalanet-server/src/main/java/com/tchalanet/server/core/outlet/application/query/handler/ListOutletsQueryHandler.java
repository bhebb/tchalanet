package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletsQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletSummaryView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListOutletsQueryHandler
    implements QueryHandler<ListOutletsQuery, TchPage<OutletSummaryView>> {

  private final OutletReaderPort reader;

  @Override
  public TchPage<OutletSummaryView> handle(ListOutletsQuery query) {
    return reader.search(query.criteria(), query.pageRequest());
  }
}
