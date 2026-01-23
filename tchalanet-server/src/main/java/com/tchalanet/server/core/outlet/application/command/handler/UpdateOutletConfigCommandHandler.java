package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.catalog.address.application.dto.AddressDto;
import com.tchalanet.server.catalog.address.application.port.out.AddressWriterPort;
import com.tchalanet.server.catalog.address.domain.model.Address;
import com.tchalanet.server.core.outlet.application.command.model.OutletConfigPatch;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateOutletConfigCommandHandler
    implements VoidCommandHandler<UpdateOutletConfigCommand> {

  private final OutletReaderPort reader;
  private final OutletWriterPort writer;
  private final AddressWriterPort addressWriter;

  @Override
  @TchTx
  public void handle(UpdateOutletConfigCommand cmd) {
    Outlet outlet = reader.getRequired(cmd.outletId(), cmd.tenantId());

    OutletConfigPatch p = cmd.patch();

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

    // handle address in patch (optional)
    AddressDto a = p.address();
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

    if (addressId != null) updated = updated.withAddressId(addressId);

    writer.save(updated);
  }
}
