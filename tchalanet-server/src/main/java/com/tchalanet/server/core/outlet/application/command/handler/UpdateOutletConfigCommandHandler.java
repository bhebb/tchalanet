package com.tchalanet.server.core.outlet.application.command.handler;

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

    var p = cmd.patch();

    LocalTime cutoff = null;
    if (p.businessDayCutoff() != null && !p.businessDayCutoff().isBlank()) {
      cutoff = LocalTime.parse(p.businessDayCutoff());
    }

    var updated =
        outlet.applyConfigPatch(
            p.salesBlocked(),
            p.salesBlockReason(),
            p.timezone(),
            cutoff,
            p.receiptPrintingEnabled(),
            p.receiptHeaderMessage(),
            p.receiptFooterMessage(),
            p.requireOpeningFloat(),
            p.autoOpenSession(),
            p.autoCloseSession(),
            p.autoSessionUserId(),
            p.autoSessionTerminalId(),
            p.defaultOpeningFloatCents(),
            Instant.now(clock));

    // handle address in patch (optional) — p.address() is domain.Address
    Address a = p.address();
    AddressId addressId = null;
    if (a != null && a.id() != null) {
      addressId = a.id();
    }

    if (addressId == null && a != null) {
      var input = new AddressInput(a.line1(), a.line2(), a.city(), a.region(), a.country(), a.postalCode());
      addressId = addressService.upsertTenantPrimary(cmd.tenantId(), input);
    }

    if (addressId != null) updated = updated.withAddressId(addressId);

    writer.save(updated);
    var event =
        new OutletConfigUpdatedEvent(
            EventId.of(idGenerator.newUuid()), Instant.now(clock), cmd.tenantId(), cmd.outletId());
    AfterCommit.run(() -> publisher.publish(event));
  }
}
