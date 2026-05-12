package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public interface TerminalSalesContextPort {
  TerminalSalesContext getContext(TerminalId terminalId, UserId sellerUserId);

  record TerminalSalesContext(
      TerminalId terminalId,
      OutletId outletId,
      boolean locked,
      boolean salesBlocked,
      boolean payoutBlocked
  ) {}
}

