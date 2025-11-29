package com.tchalanet.server.features.stats.domain.ports.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A Read Model Port to fetch ticket data required for statistical calculations. This decouples the
 * Stats domain from the Ticket domain's internal storage.
 */
public interface TicketReadModelPort {

  List<TicketInfo> findTicketsByDrawId(UUID drawId);

  /** A DTO representing the minimal ticket data needed for stats. */
  record TicketInfo(
      UUID id,
      UUID tenantId,
      BigDecimal totalAmount,
      BigDecimal totalPayout, // Calculated after the draw is resulted
      long lineCount) {}
}
