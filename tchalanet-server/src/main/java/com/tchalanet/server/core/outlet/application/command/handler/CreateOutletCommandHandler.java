package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.address.application.dto.AddressDto;
import com.tchalanet.server.core.address.application.port.out.AddressWriterPort;
import com.tchalanet.server.core.address.domain.model.Address;
import com.tchalanet.server.core.outlet.application.command.model.CreateOutletCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOutletCommandHandler implements CommandHandler<CreateOutletCommand, UUID> {

  private final OutletWriterPort writer;
  private final AddressWriterPort addressWriter;

  @Override
  @TchTx
  public UUID handle(CreateOutletCommand cmd) {
    UUID newId = OutletId.random().uuid();
    Outlet o =
        Outlet.createNew(
            TenantId.of(cmd.tenantId().uuid()), cmd.name(), cmd.slug(), OutletId.of(newId));

    AddressDto a = cmd.address();
    UUID addressId = null;
    if (a != null) addressId = a.id();
    if (addressId == null && a != null) {
      var domain =
          new Address(
              null,
              a.line1(),
              a.line2(),
              a.city(),
              a.region(),
              a.country(),
              a.postalCode(),
              a.latitude(),
              a.longitude());
      addressId = addressWriter.save(domain);
    }

    if (addressId != null) o = o.withAddressId(addressId);

    writer.save(o);
    return newId;
  }
}
