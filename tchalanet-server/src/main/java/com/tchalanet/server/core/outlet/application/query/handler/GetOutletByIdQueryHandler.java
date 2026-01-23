package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.catalog.address.application.dto.AddressDto;
import com.tchalanet.server.catalog.address.application.port.out.AddressReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletDto;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOutletByIdQueryHandler implements QueryHandler<GetOutletByIdQuery, OutletDto> {

  private final OutletReaderPort repo;
  private final AddressReaderPort addressReader;

  @Override
  public OutletDto handle(GetOutletByIdQuery q) {
    var o = repo.getRequired(q.outletId(), q.tenantId());

    AddressDto addressDto = null;
    if (o.addressId() != null) {
      var a = addressReader.findById(o.addressId()).orElse(null);
      if (a != null)
        addressDto =
            new AddressDto(
                a.id(),
                a.line1(),
                a.line2(),
                a.city(),
                a.region(),
                a.country(),
                a.postalCode(),
                a.latitude(),
                a.longitude());
    }

    return new OutletDto(
        o.id().uuid(),
        o.tenantId().uuid(),
        o.name(),
        o.slug(),
        o.dayClosed(),
        o.receiptPrintingEnabled(),
        addressDto);
  }
}
