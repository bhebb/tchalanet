package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public interface SalesTicketAdminPort {

  record TicketCloseStats(
      long total, long sold, long voided, long resultedWin, long resultedLoss, long paid) {}

  TicketCloseStats getCloseStats(OutletId outletId, Instant from, Instant to);

  void refuseNewTickets(OutletId outletId);

  void allowNewTickets(OutletId outletId);
}
