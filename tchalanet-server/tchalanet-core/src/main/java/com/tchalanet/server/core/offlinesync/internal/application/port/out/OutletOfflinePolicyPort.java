package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;

public interface OutletOfflinePolicyPort {
  OutletOfflinePolicy getPolicy(OutletId outletId);

  record OutletOfflinePolicy(boolean offlineSalesBlocked, String reason) {}
}

