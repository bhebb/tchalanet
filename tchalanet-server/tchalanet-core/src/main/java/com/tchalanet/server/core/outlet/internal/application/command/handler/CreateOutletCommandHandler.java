package com.tchalanet.server.core.outlet.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.outlet.api.command.CreateOutletCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletCreatedEvent;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import com.tchalanet.server.platform.address.api.AddressApi;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class CreateOutletCommandHandler implements CommandHandler<CreateOutletCommand, OutletId> {

    private final OutletWriterPort writer;
    private final AddressApi addressService;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public OutletId handle(CreateOutletCommand cmd) {
        var newId = OutletId.of(idGenerator.newUuid());
        var outlet = Outlet.createNew(cmd.tenantId(), cmd.name(), cmd.slug(), newId);

        var addressId = cmd.addressId();

        var input = cmd.addressInput();
        if (addressId == null && input != null) {
            addressId = addressService.upsertTenantPrimary(cmd.tenantId(), input);
        }

        if (addressId != null) {
            outlet = outlet.withAddressId(addressId);
        }

        writer.save(outlet);

        var event =
            new OutletCreatedEvent(
                EventId.of(idGenerator.newUuid()),
                Instant.now(clock),
                cmd.tenantId(),
                newId);

        AfterCommit.run(() -> publisher.publish(event));

        return newId;
    }
}
