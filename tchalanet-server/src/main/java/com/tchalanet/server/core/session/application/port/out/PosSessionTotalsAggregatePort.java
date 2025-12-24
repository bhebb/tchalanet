package com.tchalanet.server.core.session.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface PosSessionTotalsAggregatePort {

  record TotalsAggregate(
      long totalTickets,
      BigDecimal totalStake,
      BigDecimal totalPayout
  ) {}

  TotalsAggregate compute(UUID tenantId, UUID sessionId);
}
