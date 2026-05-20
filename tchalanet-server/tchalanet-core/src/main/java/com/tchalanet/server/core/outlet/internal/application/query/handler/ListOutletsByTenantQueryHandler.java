package com.tchalanet.server.core.outlet.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.api.query.ListOutletsByTenantQuery;
import com.tchalanet.server.core.outlet.api.query.OutletSummaryView;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListOutletsByTenantQueryHandler
    implements QueryHandler<ListOutletsByTenantQuery, List<OutletSummaryView>> {

    private final OutletReaderPort reader;

    @Override
    public List<OutletSummaryView> handle(ListOutletsByTenantQuery query) {
        return reader.listSummariesByTenant();
    }
}
