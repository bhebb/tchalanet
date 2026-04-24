package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.address.application.AddressCrudService;
import com.tchalanet.server.core.address.application.model.AddressInput;
import com.tchalanet.server.core.address.domain.Address;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class UpdateOutletConfigCommandHandler
    implements VoidCommandHandler<UpdateOutletConfigCommand> {

  private final OutletReaderPort reader;
  private final OutletWriterPort writer;
  private final AddressCrudService addressService;

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
            Instant.now());

    // handle address in patch (optional) — p.address() is domain.Address
    Address a = p.address();
    UUID addressId = null;
    if (a != null && a.id() != null) {
      addressId = a.id().value();
    }

    if (addressId == null && a != null) {
      var input = new AddressInput(a.line1(), a.line2(), a.city(), a.region(), a.country(), a.postalCode());
      var aid = addressService.upsertTenantPrimary(cmd.tenantId(), input);
      addressId = aid.value();
    }

    if (addressId != null) updated = updated.withAddressId(addressId);

    writer.save(updated);
  }
}
