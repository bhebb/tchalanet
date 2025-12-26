package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.List;

public interface SessionLookupPort {
  List<SessionId> findSessionIds(TenantId tenantId, OutletId outletId, Instant from, Instant to);
}

