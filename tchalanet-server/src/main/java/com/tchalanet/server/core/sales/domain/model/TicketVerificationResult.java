package com.tchalanet.server.core.sales.domain.model;

import com.tchalanet.server.common.types.enums.TicketStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.address.domain.model.Address;
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
    BigDecimal potentialTotalPayout,
    String payoutStatus,
    String outletName,
    Address outletAddress,
    List<Line> lines) {
  public record Line(
      String gameCode, String selection, BigDecimal stake, BigDecimal potentialPayout) {}
}
