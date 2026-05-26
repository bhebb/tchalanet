package com.tchalanet.server.core.outlet.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.api.query.ListSalesZonesQuery;
import com.tchalanet.server.core.outlet.api.query.SalesZoneView;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesZoneReaderPort;
import com.tchalanet.server.core.outlet.internal.domain.model.SalesZone;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListSalesZonesQueryHandler
    implements QueryHandler<ListSalesZonesQuery, List<SalesZoneView>> {

    private final SalesZoneReaderPort reader;

    @Override
    public List<SalesZoneView> handle(ListSalesZonesQuery query) {
        return reader.findAllByTenant(query.tenantId()).stream()
            .map(this::toView)
            .toList();
    }

    private SalesZoneView toView(SalesZone zone) {
        return new SalesZoneView(
            zone.id(),
            zone.tenantId(),
            zone.code(),
            zone.label(),
            zone.active(),
            zone.parentId());
    }
}
