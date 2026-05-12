package com.tchalanet.server.core.outlet.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.api.command.OutletOperationalControl;
import com.tchalanet.server.core.outlet.api.command.SetOutletOperationalControlCommand;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SetOutletOperationalControlCommandHandler
    implements CommandHandler<SetOutletOperationalControlCommand, Void> {

  private final OutletWriterPort outletWriter;
  private final Clock clock;

  @Override
  @TchTx
  public Void handle(SetOutletOperationalControlCommand cmd) {
    var now = clock.instant();

    switch (cmd.control()) {
      case SALES -> outletWriter.setSalesBlocked(cmd.outletId(), cmd.blocked(), cmd.reason(), now, cmd.performedBy());
      case PAYOUT -> outletWriter.setPayoutBlocked(cmd.outletId(), cmd.blocked(), cmd.reason(), now, cmd.performedBy());
      case OFFLINE_SALES -> outletWriter.setOfflineSalesBlocked(cmd.outletId(), cmd.blocked(), cmd.reason(), now, cmd.performedBy());
    }

    return null;
  }
}
