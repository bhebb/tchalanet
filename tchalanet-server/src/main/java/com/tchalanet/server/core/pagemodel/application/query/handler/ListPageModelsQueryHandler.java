package com.tchalanet.server.core.pagemodel.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.query.model.ListPageModelsQuery;
import com.tchalanet.server.core.pagemodel.application.query.model.PageModelSummaryView;
import lombok.RequiredArgsConstructor;

// [Phase 3B] handler absent — ListPageModelsQuery envoyée sur le bus crashait au premier appel (analysis §gap)
@UseCase
@RequiredArgsConstructor
public class ListPageModelsQueryHandler
    implements QueryHandler<ListPageModelsQuery, TchPage<PageModelSummaryView>> {

  private final PageModelReadPort readPort;

  @Override
  public TchPage<PageModelSummaryView> handle(ListPageModelsQuery query) {
    var page = readPort.search(
        query.tenantId(),
        query.scope(),
        query.logicalId(),
        query.pageable()
    );
    return TchPageMapper.map(page, inst -> new PageModelSummaryView(
        inst.id(),
        inst.logicalId(),
        inst.scope(),
        inst.slug(),
        inst.status(),
        inst.schemaVersion(),
        inst.updatedAt()
    ));
  }
}

