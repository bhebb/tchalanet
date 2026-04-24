package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

public interface SessionAdminPort {

  boolean hasOpenSessions(OutletId outletId);

  /** Close all open sessions for the outlet. Returns number of closed sessions */
  long closeAllOpenSessions(OutletId outletId, String reason);
}
