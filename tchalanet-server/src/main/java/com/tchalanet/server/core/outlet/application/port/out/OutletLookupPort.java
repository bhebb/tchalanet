package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

public interface OutletLookupPort {
  boolean isSalesBlocked(OutletId outletId);
}
