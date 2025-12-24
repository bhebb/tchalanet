package com.tchalanet.server.core.sales.domain.model;

import com.tchalanet.server.core.sales.domain.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketVerificationResult(
    UUID ticketId,
    String publicCode,
    TicketStatus status,
    UUID drawId,
    String terminalMasked,
    Instant createdAt,
    BigDecimal totalAmount,
    List<Line> lines
) {
  public record Line(
      String gameCode,
      String selection,
      BigDecimal stake,
      BigDecimal potentialPayout
  ) {}
}
