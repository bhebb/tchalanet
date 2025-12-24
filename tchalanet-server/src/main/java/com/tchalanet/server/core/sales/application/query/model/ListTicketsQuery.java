package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.sales.domain.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Query to list tickets. */
public record ListTicketsQuery(
    TicketFilter filter,
    PageRequest pageRequest
) implements Query<ListTicketsQuery.PagedResult<ListTicketsQuery.TicketSummaryDto>> {

  /** Filter for tickets. */
  public record TicketFilter(
      UUID tenantId,
      UUID terminalId, // optional
      UUID drawId, // optional
      TicketStatus status, // optional
      Instant from, // optional
      Instant to // optional
  ) {}

  /** Page request. */
  public record PageRequest(
      int page, // 0-based
      int size
  ) {}

  /** Paged result. */
  public record PagedResult<T>(List<T> items, long totalItems, int totalPages, int currentPage) {}

  /** DTO for ticket summary. */
  public record TicketSummaryDto(
      UUID id,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      String terminalLabel, // Example of resolved data
      String drawInfo // Example of resolved data
  ) {}

  /** DTO for ticket details. */
  public record TicketDetailsDto(
      UUID id,
      UUID tenantId,
      DrawRef draw,
      String ticketCode,
      String publicCode,
      TicketStatus status,
      BigDecimal totalAmount,
      Instant createdAt,
      List<TicketLineDto> lines
  ) {

    /** Reference to draw. */
    public record DrawRef(UUID id) {}

    /** DTO for ticket line. */
    public record TicketLineDto(
        String gameCode,
        String selection,
        BigDecimal stake,
        BigDecimal potentialPayout
    ) {}
  }
}
