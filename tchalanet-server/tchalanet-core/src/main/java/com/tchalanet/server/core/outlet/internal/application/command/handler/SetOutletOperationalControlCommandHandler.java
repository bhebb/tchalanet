package com.tchalanet.server.core.outlet.internal.application.command.handler.block;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.api.command.block.SetOutletOperationalControlCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SetOutletOperationalControlCommandHandler
    implements CommandHandler<SetOutletOperationalControlCommand, Void> {

  private final OutletReaderPort outletReader;
  private final OutletWriterPort outletWriter;
  private final Clock clock;

  @Override
  @TchTx
  public Void handle(SetOutletOperationalControlCommand cmd) {
    var now = clock.instant();
    Outlet outlet = outletReader.getRequired(cmd.outletId());

    Outlet updated = switch (cmd.control()) {
      case SALES -> cmd.blocked()
          ? outlet.blockSales(cmd.reason(), now, cmd.performedBy())
          : outlet.unblockSales();
      case PAYOUT -> cmd.blocked()
          ? outlet.blockPayout(cmd.reason(), now, cmd.performedBy())
          : outlet.unblockPayout();
      case OFFLINE_SALES -> cmd.blocked()
          ? outlet.blockOfflineSales(cmd.reason(), now, cmd.performedBy())
          : outlet.unblockOfflineSales();
    };

    outletWriter.save(updated);
    return null;
  }
}
