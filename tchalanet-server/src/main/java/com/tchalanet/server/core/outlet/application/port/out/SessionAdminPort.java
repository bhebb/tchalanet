package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

public interface SessionAdminPort {

  boolean hasOpenSessions(TenantId tenantId, OutletId outletId);

  /** Close all open sessions for the outlet. Returns number of closed sessions */
  long closeAllOpenSessions(TenantId tenantId, OutletId outletId, String reason);
}
