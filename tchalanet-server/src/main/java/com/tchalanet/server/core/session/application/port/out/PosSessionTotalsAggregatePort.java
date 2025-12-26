package com.tchalanet.server.core.session.application.port.out;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

public interface PosSessionTotalsAggregatePort {

  record TotalsAggregate(
      long totalTickets,
      BigDecimal totalStake,
      BigDecimal totalPayout
  ) {}

  TotalsAggregate compute(TenantId tenantId, SessionId sessionId);
}
