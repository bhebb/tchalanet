package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.common.paging.TchPageRequest;

public record ListOutletsQuery(
    OutletSearchCriteria criteria,
    TchPageRequest pageRequest
) implements Query<TchPage<OutletSummaryView>> {
}
