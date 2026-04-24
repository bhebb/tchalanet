package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.core.address.application.AddressCrudService;
import com.tchalanet.server.core.address.application.model.AddressInput;
import com.tchalanet.server.core.outlet.application.command.model.CreateOutletCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOutletCommandHandler implements CommandHandler<CreateOutletCommand, UUID> {

  private final OutletWriterPort writer;
  private final AddressCrudService addressService;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public UUID handle(CreateOutletCommand cmd) {
    UUID newId = idGenerator.newUuid();
    Outlet o =
        Outlet.createNew(
            cmd.tenantId(), cmd.name(), cmd.slug(), OutletId.of(newId));

    UUID addressId = null;
    AddressId provided = cmd.addressId();
    if (provided != null) addressId = provided.value();

    AddressInput input = cmd.addressInput();
    if (addressId == null && input != null) {
      var aid = addressService.upsertTenantPrimary(cmd.tenantId(), input);
      addressId = aid.value();
    }

    if (addressId != null) o = o.withAddressId(addressId);

    writer.save(o);
    return newId;
  }
}
