package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.core.sales.domain.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Query to get ticket details by ID. */
public record GetTicketDetailsByIdQuery(
    UUID id
) {

  /** DTO for ticket details. */
  public record TicketDetailsDto(
      UUID id,
      UUID tenantId,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      DrawInfo draw,
      List<LineInfo> lines
  ) {}

  /** DTO for draw info. */
  public record DrawInfo(UUID id, String name, Instant scheduledAt) {}

  /** DTO for line info. */
  public record LineInfo(
      String gameCode,
      String selection,
      BigDecimal stake,
      BigDecimal potentialPayout
  ) {}
}
