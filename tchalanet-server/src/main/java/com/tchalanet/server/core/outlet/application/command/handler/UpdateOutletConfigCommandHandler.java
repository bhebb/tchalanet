package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.command.model.OutletConfigPatch;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.time.Instant;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateOutletConfigCommandHandler
    implements VoidCommandHandler<UpdateOutletConfigCommand> {

  private final OutletReaderPort reader;
  private final OutletWriterPort writer;

  @Override
  @TchTx
  public void handle(UpdateOutletConfigCommand cmd) {
    Outlet outlet = reader.getRequired(cmd.tenantId(), cmd.outletId());

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

    writer.save(updated);
  }
}
