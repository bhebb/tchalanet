package com.tchalanet.server.core.tenant.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.address.application.dto.AddressDto;
import com.tchalanet.server.core.address.application.port.out.AddressReaderPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import com.tchalanet.server.core.tenant.application.query.model.GetTenantByIdQuery;
import com.tchalanet.server.core.tenant.application.query.model.TenantDto;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTenantByIdQueryHandler implements QueryHandler<GetTenantByIdQuery, TenantDto> {

  private final TenantReaderPort repo;
  private final AddressReaderPort addressReader;

  @Override
  public TenantDto handle(GetTenantByIdQuery q) {
    var t =
        repo.findById(q.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

    AddressDto addressDto = null;
    if (t.addressId() != null) {
      var a = addressReader.findById(t.addressId()).orElse(null);
      if (a != null) {
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
    }

    return new TenantDto(
        t.id().value(),
        t.code(),
        t.name(),
        t.type(),
        t.timezone(),
        t.currency(),
        t.status(),
        t.activeThemeId(),
        t.addressId(),
        addressDto);
  }
}
