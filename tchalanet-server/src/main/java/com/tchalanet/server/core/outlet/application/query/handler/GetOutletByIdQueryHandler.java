package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.address.application.model.AddressView;
import com.tchalanet.server.core.address.application.port.AddressReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOutletByIdQueryHandler implements QueryHandler<GetOutletByIdQuery, OutletView> {

  private final OutletReaderPort repo;
  private final AddressReaderPort addressReader;

  @Override
  public OutletView handle(GetOutletByIdQuery q) {
    var o = repo.getRequired(q.outletId());

    AddressView addressView = null;
    if (o.addressId() != null) {
      var a = addressReader.findById(q.tenantId(), com.tchalanet.server.common.types.id.AddressId.of(o.addressId())).orElse(null);
      if (a != null) addressView = AddressView.fromDomain(a);
    }

    return new OutletView(
        o.id().value(),
        o.tenantId() == null ? null : o.tenantId().value(),
        o.name(),
        o.slug(),
        o.dayClosed(),
        o.receiptPrintingEnabled(),
        addressView);
  }
}
