package com.tchalanet.server.core.outlet.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.api.query.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.api.query.OutletView;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.address.api.model.AddressView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOutletByIdQueryHandler implements QueryHandler<GetOutletByIdQuery, OutletView> {

    private final OutletReaderPort outletReader;
    private final AddressApi addressApi;

    @Override
    public OutletView handle(GetOutletByIdQuery query) {
        var outlet = outletReader.getRequired(query.outletId());
        var address = resolveAddress(query, outlet);

        return toView(outlet, address);
    }

    private AddressView resolveAddress(GetOutletByIdQuery query, Outlet outlet) {
        if (outlet.addressId() == null) {
            return null;
        }

        return addressApi.get(query.tenantId(), outlet.addressId()).orElse(null);
    }

    private OutletView toView(Outlet outlet, AddressView address) {
        return new OutletView(
            outlet.id(),
            outlet.tenantId(),
            outlet.name(),
            outlet.slug(),
            outlet.dayClosed(),
            outlet.receiptPrintingEnabled(),
            address);
    }
}
