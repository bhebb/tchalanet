package com.tchalanet.server.core.outlet.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.api.query.GetSalesZoneQuery;
import com.tchalanet.server.core.outlet.api.query.SalesZoneView;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesZoneReaderPort;
import com.tchalanet.server.core.outlet.internal.domain.model.SalesZone;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSalesZoneQueryHandler implements QueryHandler<GetSalesZoneQuery, SalesZoneView> {

    private final SalesZoneReaderPort reader;

    @Override
    public SalesZoneView handle(GetSalesZoneQuery query) {
        SalesZone zone = reader.getRequired(query.tenantId(), query.zoneId());
        return toView(zone);
    }

    private static SalesZoneView toView(SalesZone zone) {
        return new SalesZoneView(
            zone.id(),
            zone.tenantId(),
            zone.code(),
            zone.label(),
            zone.active(),
            zone.parentId());
    }
}
