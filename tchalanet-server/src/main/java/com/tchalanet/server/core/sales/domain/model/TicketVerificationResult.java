package com.tchalanet.server.core.sales.domain.model;

import com.tchalanet.server.common.types.enums.TicketStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TicketVerificationResult(
    TicketId ticketId,
    String publicCode,
    TicketStatus status,
    DrawId drawId,
    String terminalMasked,
    Instant createdAt,
    BigDecimal totalAmount,
    List<Line> lines) {
  public record Line(
      String gameCode, String selection, BigDecimal stake, BigDecimal potentialPayout) {}
}
