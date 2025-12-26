package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.types.enums.TicketStatus;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Query to get ticket details by public code. */
public record GetTicketDetailsByPublicCodeQuery(String publicCode) {

  /** DTO for ticket details. */
  public record TicketDetailsDto(
      UUID id,
      TenantId tenantId,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      DrawInfo draw,
      List<LineInfo> lines) {}

  /** DTO for draw info. */
  public record DrawInfo(UUID id, String name, Instant scheduledAt) {}

  /** DTO for line info. */
  public record LineInfo(
      String gameCode, String selection, BigDecimal stake, BigDecimal potentialPayout) {}
}
