package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.command.model.ReopenOutletDayCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReopenOutletDayCommandHandler implements VoidCommandHandler<ReopenOutletDayCommand> {

  private final OutletReaderPort reader;
  private final OutletWriterPort writer;

  @Override
  @TchTx
  public void handle(ReopenOutletDayCommand cmd) {
    Outlet outlet = reader.getRequired(cmd.tenantId(), cmd.outletId());
    Outlet updated = outlet.reopenDay();
    writer.save(updated);
  }
}
