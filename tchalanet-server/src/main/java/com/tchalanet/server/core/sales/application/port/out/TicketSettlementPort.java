package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TicketSettlementPort {
  /**
   * Returns SOLD tickets for a given draw, ordered by createdAt then id.
   * Must include lines (for winning calculation).
   */
  List<Ticket> findNextBatchForDraw(
      TenantId tenantId,
      DrawId drawId,
      Instant afterCreatedAt,
      UUID afterId,
      int limit);
}
