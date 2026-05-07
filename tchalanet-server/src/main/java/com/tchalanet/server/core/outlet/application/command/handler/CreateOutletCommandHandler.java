package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.address.application.AddressCrudService;
import com.tchalanet.server.core.address.application.model.AddressInput;
import com.tchalanet.server.core.outlet.application.command.model.CreateOutletCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOutletCommandHandler implements CommandHandler<CreateOutletCommand, OutletId> {

    private final OutletWriterPort writer;
    private final AddressCrudService addressService;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public OutletId handle(CreateOutletCommand cmd) {
        var newId = OutletId.of(idGenerator.newUuid());
        var outlet = Outlet.createNew(cmd.tenantId(), cmd.name(), cmd.slug(), newId);

        var addressId = cmd.addressId();

        AddressInput input = cmd.addressInput();
        if (addressId == null && input != null) {
            addressId = addressService.upsertTenantPrimary(cmd.tenantId(), input);
        }

        if (addressId != null) {
            outlet = outlet.withAddressId(addressId);
        }

        writer.save(outlet);
        return newId;
    }
}
