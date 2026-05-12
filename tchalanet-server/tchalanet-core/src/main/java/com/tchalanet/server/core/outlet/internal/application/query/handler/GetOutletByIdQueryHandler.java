package com.tchalanet.server.core.outlet.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.address.api.model.AddressView;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletView;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOutletByIdQueryHandler implements QueryHandler<GetOutletByIdQuery, OutletView> {

    private final OutletReaderPort outletReader;
    private final AddressReaderPort addressReader;

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

        return addressReader
            .findById(query.tenantId(), outlet.addressId())
            .map(AddressView::fromDomain)
            .orElse(null);
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
