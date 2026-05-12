package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TicketSettlementPort {
  /**
   * Returns SOLD tickets for a given draw, ordered by createdAt then id.
   * Must include lines (for winning calculation).
   */
  List<Ticket> findNextBatchForDraw(
      DrawId drawId,
      Instant afterCreatedAt,
      UUID afterId,
      int limit);
}
