package com.tchalanet.server.ticket.web.dto;

import com.tchalanet.server.ticket.domain.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    UUID tenantId,
    UUID terminalId,
    UUID drawId,
    String ticketCode,
    String publicCode,
    TicketStatus status,
    BigDecimal totalAmount,
    Instant createdAt,
    Instant updatedAt,
    List<LineResponse> lines) {
  public record LineResponse(
      String gameCode,
      String selection,
      BigDecimal stake,
      BigDecimal oddsSnapshot,
      BigDecimal potentialPayout) {}
}
