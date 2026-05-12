package com.tchalanet.server.core.outlet.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.address.application.AddressCrudService;
import com.tchalanet.server.core.address.application.model.AddressInput;
import com.tchalanet.server.core.address.domain.Address;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.domain.event.OutletConfigUpdatedEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;

@UseCase
@RequiredArgsConstructor
public class UpdateOutletConfigCommandHandler
    implements VoidCommandHandler<UpdateOutletConfigCommand> {

    private final OutletReaderPort reader;
    private final OutletWriterPort writer;
    private final AddressCrudService addressService;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(UpdateOutletConfigCommand cmd) {
        var outlet = reader.getRequired(cmd.outletId());
        var patch = cmd.patch();
        var now = Instant.now(clock);

        var updated =
            outlet.applyConfigPatch(
                patch.salesBlocked(),
                patch.salesBlockReason(),
                patch.timezone(),
                patch.receiptPrintingEnabled(),
                patch.receiptHeaderMessage(),
                patch.receiptFooterMessage(),
                patch.requireOpeningFloat(),
                patch.autoSessionOpenEnabled(),
                patch.autoSessionCloseEnabled(),
                parseLocalTime(patch.sessionOpenTime()),
                parseLocalTime(patch.sessionCloseTime()),
                patch.defaultOpeningFloatCents(),
                now);

        var addressId = resolveAddressId(cmd, patch.address());
        if (addressId != null) {
            updated = updated.withAddressId(addressId);
        }

        writer.save(updated);

        var event =
            new OutletConfigUpdatedEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                cmd.tenantId(),
                cmd.outletId());

        AfterCommit.run(() -> publisher.publish(event));
    }

    private LocalTime parseLocalTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value);
    }

    private AddressId resolveAddressId(UpdateOutletConfigCommand cmd, Address address) {
        if (address == null) {
            return null;
        }

        if (address.id() != null) {
            return address.id();
        }

        var input =
            new AddressInput(
                address.line1(),
                address.line2(),
                address.city(),
                address.region(),
                address.country(),
                address.postalCode());

        return addressService.upsertTenantPrimary(cmd.tenantId(), input);
    }
}
