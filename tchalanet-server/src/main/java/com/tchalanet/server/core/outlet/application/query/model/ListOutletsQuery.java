package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

public record ListOutletsQuery(
    OutletSearchCriteria criteria,
    TchPageRequest pageRequest
) implements Query<TchPage<OutletSummaryView>> {
}
