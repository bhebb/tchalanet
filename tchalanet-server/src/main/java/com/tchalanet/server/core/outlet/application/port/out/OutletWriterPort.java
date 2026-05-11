package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import com.tchalanet.server.core.outlet.domain.model.OutletStatus;

import java.time.Instant;

public interface OutletWriterPort {
  void save(Outlet outlet);
    void setStatus(OutletId outletId, OutletStatus status, String reason, Instant at, UserId performedBy);

    void setSalesBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy);

    void setPayoutBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy);

    void setOfflineSalesBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy);
}
